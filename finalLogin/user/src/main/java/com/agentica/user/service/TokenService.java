package com.agentica.user.service;

import com.agentica.user.domain.token.RefreshToken;
import com.agentica.user.domain.token.RefreshTokenRepository;
import com.agentica.user.dto.TokenResponse;
import com.agentica.user.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    // 토큰 생성
    public TokenResponse createTokens(String email) {
        String accessToken = jwtTokenProvider.createAccessToken(email);
        String refreshToken = jwtTokenProvider.createRefreshToken(email);

        // Refresh Token 저장
        saveRefreshToken(email, refreshToken);

        return TokenResponse.of(accessToken, refreshToken, jwtTokenProvider.getAccessTokenValidityTime());
    }

    // Refresh Token으로 Access Token 갱신
    public TokenResponse refreshToken(String refreshToken) {
        // Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken);

        // DB에서 Refresh Token 확인
        RefreshToken savedToken = refreshTokenRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Refresh Token입니다."));

        if (!savedToken.getToken().equals(refreshToken) || savedToken.isExpired()) {
            throw new IllegalArgumentException("만료되거나 유효하지 않은 Refresh Token입니다.");
        }

        // 새로운 토큰 생성
        return createTokens(email);
    }    // 로그아웃 (Refresh Token 삭제)
    public void logout(String email) {
        log.info("RefreshToken 삭제 시작: {}", email);
        try {
            refreshTokenRepository.deleteByEmail(email);
            log.info("RefreshToken 삭제 완료: {}", email);
        } catch (Exception e) {
            log.error("RefreshToken 삭제 중 오류 발생: {}", email, e);
        }
    }
    
    // 강화된 로그아웃 - 모든 관련 토큰 완전 삭제
    public void forceLogout(String email) {
        log.info("=== 강화된 로그아웃 - 모든 토큰 삭제 시작: {} ===", email);
        try {
            // 1. RefreshToken 삭제
            refreshTokenRepository.deleteByEmail(email);
            log.info("RefreshToken 삭제 완료");
            
            // 2. 추가적인 토큰 정리 로직 (필요시 확장 가능)
            // 예: 블랙리스트에 추가, 캐시 정리 등
            
            log.info("강화된 로그아웃 완료: {}", email);
        } catch (Exception e) {
            log.error("강화된 로그아웃 중 오류 발생: {}", email, e);
        }
        log.info("=== 강화된 로그아웃 완료: {} ===", email);
    }

    private void saveRefreshToken(String email, String refreshToken) {
        refreshTokenRepository.findByEmail(email)
                .ifPresentOrElse(
                        token -> token.updateToken(refreshToken, LocalDateTime.now().plusDays(7)),
                        () -> {
                            RefreshToken newToken = RefreshToken.builder()
                                    .email(email)
                                    .token(refreshToken)
                                    .expiresAt(LocalDateTime.now().plusDays(7))
                                    .build();
                            refreshTokenRepository.save(newToken);
                        }
                );
    }
}
