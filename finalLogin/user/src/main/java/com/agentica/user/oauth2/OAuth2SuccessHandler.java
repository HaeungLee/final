package com.agentica.user.oauth2;

import com.agentica.user.domain.member.Member;
import com.agentica.user.domain.token.RefreshToken;
import com.agentica.user.domain.token.RefreshTokenRepository;
import com.agentica.user.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);
      @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException {
        
        log.info("=== 🎉 OAuth2 로그인 성공 핸들러 실행 ===");
        log.info("요청 URL: {}", request.getRequestURL());
        log.info("요청 URI: {}", request.getRequestURI());
        
        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
        Member member = oauth2User.getMember();
        
        log.info("🔑 OAuth2 Provider: {}", member.getProvider());
        log.info("👤 사용자 정보 - Email: {}, Name: {}", member.getEmail(), member.getName());
        
        // 네이버 특별 처리 로깅
        if (member.getProvider() == com.agentica.user.domain.member.AuthProvider.NAVER) {
            log.info("🟢 네이버 OAuth2 로그인 성공 - 특별 처리 시작");
        }
        
        String accessToken = jwtTokenProvider.createAccessToken(member.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getEmail());
        
        log.info("🎫 JWT 토큰 생성 완료 - Access Token 길이: {}", accessToken.length());
        
        // Refresh Token을 데이터베이스에 저장
        saveRefreshToken(member.getEmail(), refreshToken);
        
        // HTTP-Only 쿠키에 토큰 저장 (보안을 위해)
        response.addCookie(createCookie("accessToken", accessToken, 1800)); // 30분
        response.addCookie(createCookie("refreshToken", refreshToken, 604800)); // 7일
        
        try {
            // URL 인코딩으로 한글 이름 안전하게 처리
            String encodedName = URLEncoder.encode(member.getName(), StandardCharsets.UTF_8);
            String encodedEmail = URLEncoder.encode(member.getEmail(), StandardCharsets.UTF_8);
            String provider = member.getProvider().name();
            
            String redirectUrl = String.format(
                "http://localhost:5173/?login=success&email=%s&name=%s&provider=%s",
                encodedEmail, encodedName, provider
            );
            
            log.info("🚀 React 클라이언트로 리다이렉트: {}", redirectUrl);
            response.sendRedirect(redirectUrl);
            
        } catch (Exception e) {
            log.error("❌ 리다이렉트 중 오류 발생: {}", e.getMessage(), e);
            // 오류 발생 시 기본 리다이렉트 (이름 없이)
            String fallbackUrl = String.format(
                "http://localhost:5173/?login=success&email=%s&provider=%s",
                URLEncoder.encode(member.getEmail(), StandardCharsets.UTF_8),
                member.getProvider().name()
            );
            response.sendRedirect(fallbackUrl);
        }
        
        log.info("=== ✅ OAuth2 로그인 성공 핸들러 완료 ===");
    }
    
    private jakarta.servlet.http.Cookie createCookie(String name, String value, int maxAge) {
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // 개발환경에서는 false, 운영환경에서는 true
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        return cookie;
    }
    
    private void saveRefreshToken(String email, String refreshToken) {
        refreshTokenRepository.findByEmail(email)
                .ifPresentOrElse(
                    token -> token.updateToken(refreshToken, 
                        LocalDateTime.now().plusDays(7)), // 7일 만료
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
