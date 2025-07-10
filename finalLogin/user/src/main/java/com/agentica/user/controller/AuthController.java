package com.agentica.user.controller;

import com.agentica.user.dto.*;
import com.agentica.user.response.ApiResponse;
import com.agentica.user.service.MemberService;
import com.agentica.user.service.AuthService;
import com.agentica.user.service.VerificationService;
import com.agentica.user.service.TokenService;
import com.agentica.user.service.SocialLogoutService;
import com.agentica.user.service.DeleteAccountTokenService;
import com.agentica.user.config.SocialLogoutConfig;
import com.agentica.user.domain.member.AuthProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final MemberService memberService;
    private final AuthService authService;
    private final VerificationService verificationService;
    private final TokenService tokenService;
    private final SocialLogoutService socialLogoutService;
    private final SocialLogoutConfig socialLogoutConfig;
    private final DeleteAccountTokenService deleteAccountTokenService;
    
    // 이메일 인증번호 전송
    @PostMapping("/send-verification-code")
    public ResponseEntity<ApiResponse<String>> sendVerificationCode(@Valid @RequestBody EmailVerificationRequest request) {
        try {
            verificationService.sendVerificationCode(request);
            return ResponseEntity.ok(ApiResponse.success("인증번호가 전송되었습니다. 이메일을 확인해주세요."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    // 인증번호 확인
    @PostMapping("/verify-code")
    public ResponseEntity<ApiResponse<String>> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        try {
            verificationService.verifyCode(request);
            return ResponseEntity.ok(ApiResponse.success("이메일 인증이 완료되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }    }

    // 이메일 중복 확인
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkEmail(@RequestParam String email) {
        try {
            boolean exists = authService.isEmailExists(email);
            Map<String, Boolean> result = new HashMap<>();
            result.put("exists", exists);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("이메일 확인 중 오류가 발생했습니다."));
        }
    }

    // 회원가입
    // @PostMapping("/join")
    // public ResponseEntity<ApiResponse<String>> join(@Valid @RequestBody JoinRequest joinRequest) {
    //     try {
    //         authService.join(joinRequest);
    //         return ResponseEntity.ok(ApiResponse.success("회원가입이 완료되었습니다."));
    //     } catch (IllegalArgumentException e) {
    //         return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    //     }
    // }
    @PostMapping("/join")
public ResponseEntity<ApiResponse<String>> join(@Valid @RequestBody JoinRequest joinRequest) {
    try {
        log.info("[회원가입 요청] {}", joinRequest.toSafeLog());
        authService.join(joinRequest);
        return ResponseEntity.ok(ApiResponse.success("회원가입이 완료되었습니다."));
    } catch (IllegalArgumentException e) {
        log.warn("[회원가입 실패] {} - {}", joinRequest.toSafeLog(), e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }
}    // 로그인
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest loginRequest, 
                                                           HttpServletResponse response) {
        try {
            Authentication authentication = authService.login(loginRequest);
            String email = authentication.getName();
            TokenResponse tokenResponse = tokenService.createTokens(email);
            
            // 쿠키에 토큰 저장 (소셜 로그인과 동일하게)
            addTokenCookies(response, tokenResponse);
            
            return ResponseEntity.ok(ApiResponse.success(tokenResponse));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 토큰을 HTTP-Only 쿠키에 저장
     */
    private void addTokenCookies(HttpServletResponse response, TokenResponse tokenResponse) {
        // Access Token 쿠키 (30분)
        jakarta.servlet.http.Cookie accessTokenCookie = new jakarta.servlet.http.Cookie("accessToken", tokenResponse.getAccessToken());
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(false); // 개발환경
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(1800); // 30분
        response.addCookie(accessTokenCookie);
        
        // Refresh Token 쿠키 (7일)
        jakarta.servlet.http.Cookie refreshTokenCookie = new jakarta.servlet.http.Cookie("refreshToken", tokenResponse.getRefreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false); // 개발환경
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(604800); // 7일
        response.addCookie(refreshTokenCookie);
        
        log.info("로그인 토큰 쿠키 저장 완료");
    }

    // 토큰 갱신
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            TokenResponse tokenResponse = tokenService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(ApiResponse.success(tokenResponse));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }    }

    // 비밀번호 확인
    @PostMapping("/verify-password")
    public ResponseEntity<ApiResponse<String>> verifyPassword(
            @Valid @RequestBody PasswordVerifyRequest request,
            Authentication authentication) {
        try {
            log.info("=== 비밀번호 확인 API 호출됨 ===");
            log.info("사용자: {}", authentication.getName());
            
            boolean isValid = memberService.verifyPassword(authentication.getName(), request.getPassword());
            
            if (isValid) {
                log.info("비밀번호 확인 성공: {}", authentication.getName());
                return ResponseEntity.ok(ApiResponse.success("비밀번호 확인 완료"));
            } else {
                log.warn("비밀번호 확인 실패: {}", authentication.getName());
                return ResponseEntity.badRequest().body(ApiResponse.error("현재 비밀번호가 올바르지 않습니다."));
            }
        } catch (Exception e) {
            log.error("비밀번호 확인 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("비밀번호 확인 중 오류가 발생했습니다."));
        }
    }    // 일반 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {
        try {
            if (authentication != null) {
                String userEmail = authentication.getName();
                log.info("로그아웃 요청 사용자: {}", userEmail);
                
                // 소셜 로그아웃 처리
                socialLogoutService.processLogout(authentication, userEmail);
                
                // Spring Security 로그아웃
                new SecurityContextLogoutHandler().logout(request, response, authentication);
                
                // 세션 무효화
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                  // 쿠키 삭제
                deleteAllCookies(response, request);
            }

            return ResponseEntity.ok(ApiResponse.success("로그아웃되었습니다."));
        } catch (Exception e) {
            log.error("로그아웃 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("로그아웃 중 오류가 발생했습니다."));
        }
    }    /**
     * 쿠키 삭제 유틸리티 메서드 - 개선된 안전한 버전
     */
    private void deleteAllCookies(HttpServletResponse response, HttpServletRequest request) {
        log.info("🧹 안전한 쿠키 삭제 시작");
        
        // 기본 쿠키들
        String[] cookieNames = {"JSESSIONID", "refreshToken", "accessToken", "PGLADMIN_LANGUAGE", 
                               "jwt_token", "auth_token", "remember-me", "SESSION"};
        
        // 1단계: 기본 경로별 쿠키 삭제 (도메인 설정 없음 - 가장 안전한 방법)
        for (String name : cookieNames) {
            String[] paths = {"/", "/api", "/user", "/oauth2", "/login"};
            for (String path : paths) {
                deleteCookieSafely(response, name, path);
            }
        }
        
        // 2단계: 서버 호스트명 기반 도메인 쿠키 삭제 (localhost 제외)
        String serverName = request.getServerName();
        log.info("🌐 서버 호스트명: {}", serverName);
        
        if (serverName != null && !serverName.equals("localhost") && !serverName.equals("127.0.0.1")) {
            // 실제 도메인인 경우에만 도메인 쿠키 삭제 시도
            for (String name : cookieNames) {
                deleteCookieWithValidDomain(response, name, serverName);
            }
        } else {
            log.info("⏭️ localhost/127.0.0.1 환경 - 도메인 쿠키 삭제 건너뜀");
        }
        
        log.info("✅ 쿠키 삭제 완료");
    }    /**
     * 쿠키 삭제 (특별 쿠키 처리 포함) - 강화된 버전
     */
    private void deleteCookieSafely(HttpServletResponse response, String name, String path) {
        try {
            // 특별 처리가 필요한 쿠키들
            if ("JSESSIONID".equals(name) || "accessToken".equals(name) || "refreshToken".equals(name)) {
                deleteSpecialCookieInAuth(response, name);
                return;
            }
            
            // 일반 쿠키 삭제
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(name, "");
            cookie.setPath(path);
            cookie.setMaxAge(0);
            cookie.setHttpOnly(true);
            cookie.setSecure(false);  // 개발환경 설정과 일치
            response.addCookie(cookie);
            log.debug("✅ 일반 쿠키 삭제 성공: {} (경로: {})", name, path);
        } catch (Exception e) {
            log.warn("⚠️ 쿠키 삭제 실패: {} (경로: {}) - {}", name, path, e.getMessage());
        }
    }
      /**
     * 특별 쿠키 삭제 (AuthController 버전) - 정확한 속성 매칭
     */
    private void deleteSpecialCookieInAuth(HttpServletResponse response, String name) {
        log.info("🔥 AuthController: 정확한 속성으로 특별 쿠키 삭제 시작: {}", name);
        
        try {
            // OAuth2SuccessHandler에서 생성한 속성과 정확히 일치하도록 삭제
            if ("accessToken".equals(name) || "refreshToken".equals(name)) {
                jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(name, "");
                cookie.setHttpOnly(true);   // 생성 시와 동일
                cookie.setSecure(false);    // 개발환경 설정과 동일
                cookie.setPath("/");        // 생성 시와 동일
                cookie.setMaxAge(0);        // 삭제를 위해 0으로 설정
                response.addCookie(cookie);
                
                log.info("✅ AuthController: {} 쿠키 삭제 (정확한 속성 매칭): HttpOnly=true, Secure=false, Path=/", name);
            }
            
            if ("JSESSIONID".equals(name)) {
                // JSESSIONID 삭제 (개발환경 설정 고려)
                String[] paths = {"/", ""};
                boolean[] httpOnlyOptions = {true, false};
                
                for (String path : paths) {
                    for (boolean httpOnly : httpOnlyOptions) {
                        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(name, "");
                        cookie.setHttpOnly(httpOnly);
                        cookie.setSecure(false);    // 개발환경
                        cookie.setPath(path);
                        cookie.setMaxAge(0);
                        response.addCookie(cookie);
                    }
                }
                
                log.info("✅ AuthController: JSESSIONID 쿠키 삭제 완료");
            }
            
            // 추가 보장을 위한 Set-Cookie 헤더
            response.addHeader("Set-Cookie", 
                String.format("%s=; Path=/; Max-Age=0; HttpOnly; Expires=Thu, 01 Jan 1970 00:00:00 GMT", name));
            
            log.info("✅ AuthController: 특별 쿠키 삭제 완료: {}", name);
            
        } catch (Exception e) {
            log.error("❌ AuthController: 특별 쿠키 삭제 중 오류: {}", name, e);
        }
    }
    
    /**
     * 쿠키 삭제 (유효한 도메인 지정)
     */
    private void deleteCookieWithValidDomain(HttpServletResponse response, String name, String domain) {
        try {
            // RFC 6265 준수: 유효한 도메인만 설정
            if (isValidCookieDomain(domain)) {
                jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(name, "");
                cookie.setPath("/");
                cookie.setMaxAge(0);
                cookie.setHttpOnly(true);
                cookie.setSecure(false);
                cookie.setDomain(domain);
                response.addCookie(cookie);
                log.debug("✅ 도메인 쿠키 삭제 성공: {} (도메인: {})", name, domain);
            } else {
                log.debug("⏭️ 유효하지 않은 도메인 건너뜀: {}", domain);
            }
        } catch (Exception e) {
            log.warn("⚠️ 도메인 쿠키 삭제 실패: {} (도메인: {}) - {}", name, domain, e.getMessage());
        }
    }
    
    /**
     * 유효한 쿠키 도메인 검증
     */
    private boolean isValidCookieDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return false;
        }
        
        // localhost 관련 도메인은 유효하지 않음
        if (domain.equals("localhost") || domain.equals("127.0.0.1") || 
            domain.equals(".localhost") || domain.equals(".127.0.0.1")) {
            return false;
        }
        
        // 실제 도메인만 허용 (점으로 시작하는 경우 포함)
        return domain.contains(".") && !domain.startsWith(".");
    }    // 소셜 로그아웃 처리 - 심플 버전
    @PostMapping("/social-logout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> socialLogout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {
            
        try {
            log.info("=== 소셜 로그아웃 API 호출 ===");
            
            // 🔥 [수정] 먼저 소셜 로그아웃 처리 (세션 무효화 전에)
            var provider = socialLogoutService.getProvider(authentication);
            String userEmail = authentication != null ? authentication.getName() : null;
            
            log.info("🔍 로그아웃 처리 전 상태 확인:");
            log.info("  - Authentication 타입: {}", authentication != null ? authentication.getClass().getSimpleName() : "null");
            log.info("  - 사용자 이메일: {}", userEmail);
            log.info("  - 제공자: {}", provider);
            
            // 2. 소셜 로그아웃 처리 (Authentication이 유효한 상태에서)
            socialLogoutService.processLogout(authentication, userEmail);
            
            // 3. 세션 무효화 및 쿠키 삭제 (소셜 로그아웃 처리 후)
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            deleteAllCookies(response, request);
            
            // 4. 응답 데이터 준비
            Map<String, Object> result = new HashMap<>();
            result.put("actualProvider", provider.name());
            result.put("requiresSocialLogout", socialLogoutService.needsSocialLogout(provider));
            result.put("clearStorage", true);
            
            if (socialLogoutService.needsSocialLogout(provider)) {
                String logoutUrl = socialLogoutService.getSocialLogoutUrl(provider);
                
                if (provider == AuthProvider.KAKAO) {
                    result.put("backgroundLogout", true);
                    result.put("message", "카카오 백그라운드 로그아웃 처리됨");
                } else {
                    result.put("logoutUrl", logoutUrl);
                    result.put("redirectLogout", true);
                    result.put("message", provider.name() + " 로그아웃 페이지로 이동합니다.");
                }
            } else {
                result.put("message", "일반 로그아웃 처리됨");
            }
            
            log.info("소셜 로그아웃 완료: {} ({})", userEmail, provider);
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("소셜 로그아웃 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("소셜 로그아웃 중 오류가 발생했습니다."));
        }
    }
    
    // 강제 로그아웃 (추가 보장)
    @PostMapping("/force-logout")
    public ResponseEntity<ApiResponse<String>> forceLogout(
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            log.info("강제 로그아웃 요청");
            
            // 세션 무효화
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
              // 모든 쿠키 강제 삭제
            deleteAllCookies(response, request);
            
            // 추가 쿠키 삭제 (더 많은 경로에서)
            String[] extraCookies = {"JSESSIONID", "refreshToken", "accessToken", "PGLADMIN_LANGUAGE", "jwt_token", "auth_token"};
            for (String cookieName : extraCookies) {
                // 다양한 경로에서 삭제 시도
                String[] paths = {"/", "/api", "/user", ""};
                for (String path : paths) {
                    jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(cookieName, "");
                    cookie.setPath(path);
                    cookie.setMaxAge(0);
                    cookie.setHttpOnly(true);
                    cookie.setSecure(false);  // 개발환경
                    response.addCookie(cookie);
                }
            }
            
            log.info("강제 로그아웃 완료");
            return ResponseEntity.ok(ApiResponse.success("강제 로그아웃 완료"));
        } catch (Exception e) {
            log.error("강제 로그아웃 중 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("강제 로그아웃 중 오류가 발생했습니다."));
        }
    }    // 소셜 사용자 비밀번호 설정 (소셜 → 일반 사용자 전환)
    @PostMapping("/set-password")
    public ResponseEntity<ApiResponse<String>> setSocialUserPassword(
            @Valid @RequestBody SetPasswordRequest request,
            Authentication authentication) {
        try {
            log.info("=== 비밀번호 설정 API 호출됨 ===");
            log.info("사용자: {}", authentication.getName());
            log.info("요청 데이터: 비밀번호 길이={}, 확인 일치={}", 
                    request.getPassword() != null ? request.getPassword().length() : 0,
                    request.isPasswordConfirmed());
            
            // 비밀번호 확인 검증
            if (!request.isPasswordConfirmed()) {
                log.warn("비밀번호 확인 불일치");
                return ResponseEntity.badRequest().body(ApiResponse.error("비밀번호와 비밀번호 확인이 일치하지 않습니다."));
            }
            
            log.info("비밀번호 설정 서비스 호출 시작");
            memberService.setSocialUserPassword(authentication.getName(), request.getPassword());
            
            log.info("소셜 사용자 비밀번호 설정 완료: {} → LOCAL 전환", authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success("비밀번호가 설정되었습니다. 이제 일반 로그인도 가능합니다."));
        } catch (IllegalArgumentException e) {
            log.error("비밀번호 설정 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("비밀번호 설정 중 예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("비밀번호 설정 중 오류가 발생했습니다."));
        }
    }

    // 프로필 업데이트
    @PostMapping("/update-profile")
    public ResponseEntity<ApiResponse<String>> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            Authentication authentication) {
        try {
            log.info("=== 프로필 업데이트 API 호출됨 ===");
            log.info("사용자: {}", authentication.getName());
            log.info("이름 변경: {}, 비밀번호 변경: {}", 
                    request.getName(), 
                    request.getPassword() != null && !request.getPassword().isEmpty());
            
            // 현재 비밀번호 확인 (보안상 재확인)
            boolean isCurrentPasswordValid = memberService.verifyPassword(
                    authentication.getName(), 
                    request.getCurrentPassword());
            
            if (!isCurrentPasswordValid) {
                log.warn("프로필 업데이트 실패 - 현재 비밀번호 불일치: {}", authentication.getName());
                return ResponseEntity.badRequest().body(ApiResponse.error("현재 비밀번호가 올바르지 않습니다."));
            }
            
            // 프로필 업데이트 실행
            memberService.updateProfile(authentication.getName(), request);
            
            log.info("프로필 업데이트 완료: {}", authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success("프로필이 성공적으로 수정되었습니다."));
        } catch (IllegalArgumentException e) {
            log.error("프로필 업데이트 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));        } catch (Exception e) {
            log.error("프로필 업데이트 중 예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("프로필 업데이트 중 오류가 발생했습니다."));
        }
    }    // 개선된 회원탈퇴 - 시작 단계
    @PostMapping("/initiate-delete-account")
    public ResponseEntity<ApiResponse<Map<String, String>>> initiateDeleteAccount(
            @Valid @RequestBody DeleteAccountRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        try {
            log.info("=== 회원탈퇴 시작 API 호출됨 ===");
            log.info("사용자: {}", authentication.getName());
            
            // 현재 비밀번호 확인 (보안 검증)
            boolean isCurrentPasswordValid = memberService.verifyPassword(
                    authentication.getName(), 
                    request.getCurrentPassword());
            
            if (!isCurrentPasswordValid) {
                log.warn("회원탈퇴 시작 실패 - 현재 비밀번호 불일치: {}", authentication.getName());
                return ResponseEntity.badRequest().body(ApiResponse.error("현재 비밀번호가 올바르지 않습니다."));
            }
            
            Map<String, String> response = new HashMap<>();
            
            // 소셜 로그인 사용자인지 확인
            if (authentication instanceof OAuth2AuthenticationToken) {
                OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
                String provider = oauthToken.getAuthorizedClientRegistrationId();
                log.info("소셜 로그인 사용자 탈퇴 시작 - 제공업체: {}", provider);
                
                // 임시 토큰 생성
                String deleteToken = deleteAccountTokenService.createDeleteToken(authentication.getName());
                
                // 소셜 로그아웃 URL 생성
                String logoutUrl = socialLogoutService.generateDeleteAccountLogoutUrl(provider, deleteToken);
                
                if (logoutUrl != null) {
                    response.put("type", "social");
                    response.put("provider", provider);
                    response.put("logoutUrl", logoutUrl);
                    response.put("message", "소셜 로그아웃 후 회원탈퇴가 완료됩니다.");
                    
                    log.info("소셜 로그아웃 URL 생성 완료: {}", provider);
                    return ResponseEntity.ok(ApiResponse.success(response));
                } else {
                    log.warn("소셜 로그아웃 URL 생성 실패 - 일반 탈퇴로 처리: {}", provider);
                }
            }
              // 일반 사용자 또는 소셜 URL 생성 실패 시 바로 삭제 처리
            log.info("일반 사용자 회원탈퇴 처리: {}", authentication.getName());
            
            // 회원 정보 삭제
            memberService.deleteMember(authentication.getName());
            
            // 세션 무효화 및 로그아웃 처리
            HttpSession session = httpRequest.getSession(false);
            if (session != null) {
                try {
                    session.invalidate();
                    log.info("세션 무효화 완료");
                } catch (Exception e) {
                    log.warn("세션 무효화 중 오류: {}", e.getMessage());
                }
            }
            
            // 모든 쿠키 정리
            deleteAllCookies(httpResponse, httpRequest);
            
            // Spring Security 로그아웃 처리
            SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
            logoutHandler.logout(httpRequest, httpResponse, authentication);
            
            response.put("type", "local");
            response.put("message", "회원탈퇴가 완료되었습니다. 그동안 이용해주셔서 감사합니다.");
            
            log.info("일반 회원탈퇴 완료: {}", authentication.getName());
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (IllegalArgumentException e) {
            log.error("회원탈퇴 시작 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("회원탈퇴 시작 중 예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("회원탈퇴 중 오류가 발생했습니다."));
        }
    }
    
    // 소셜 로그아웃 콜백 후 DB 삭제 처리
    @GetMapping("/complete-delete-account")
    public String completeDeleteAccount(
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String token,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        log.info("=== 회원탈퇴 완료 처리 ===");
        log.info("제공자: {}, 토큰: {}", provider, token != null ? token.substring(0, 8) + "..." : "null");
        
        try {
            // 임시 토큰으로 사용자 식별
            String email = deleteAccountTokenService.validateAndGetEmail(token);
            if (email == null) {
                log.warn("유효하지 않은 토큰 - 로그인 페이지로 리다이렉트");
                return "redirect:/login?error=invalid_token";
            }
            
            log.info("토큰 검증 성공 - 사용자: {}", email);
            
            // 세션 무효화 (안전한 방식)
            HttpSession session = request.getSession(false);
            if (session != null) {
                try {
                    session.invalidate();
                    log.info("세션 무효화 완료");
                } catch (Exception e) {
                    log.warn("세션 무효화 중 오류: {}", e.getMessage());
                }
            }
            
            // 모든 쿠키 정리
            deleteAllCookies(response, request);
            
            // DB에서 사용자 삭제
            memberService.deleteMember(email);
            log.info("사용자 삭제 완료: {}", email);
            
            // 토큰 제거
            deleteAccountTokenService.removeToken(token);
            
            // 탈퇴 완료 페이지로 리다이렉트
            return "redirect:/delete-account-complete?provider=" + provider;
            
        } catch (Exception e) {
            log.error("회원탈퇴 완료 처리 중 오류 발생", e);
            return "redirect:/error";
        }
    }
    
    // 기존 회원탈퇴 API (호환성 유지)
    @DeleteMapping("/delete-account")
    public ResponseEntity<ApiResponse<String>> deleteAccount(
            @Valid @RequestBody DeleteAccountRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        try {
            log.info("=== 기존 회원탈퇴 API 호출됨 (호환성 유지) ===");
            log.info("사용자: {}", authentication.getName());
            
            // 현재 비밀번호 확인 (보안 검증)
            boolean isCurrentPasswordValid = memberService.verifyPassword(
                    authentication.getName(), 
                    request.getCurrentPassword());
            
            if (!isCurrentPasswordValid) {
                log.warn("회원탈퇴 실패 - 현재 비밀번호 불일치: {}", authentication.getName());
                return ResponseEntity.badRequest().body(ApiResponse.error("현재 비밀번호가 올바르지 않습니다."));
            }
            
            // 소셜 로그인 사용자인지 확인하여 소셜 로그아웃도 처리
            if (authentication instanceof OAuth2AuthenticationToken) {
                OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
                String provider = oauthToken.getAuthorizedClientRegistrationId();
                log.info("소셜 로그인 사용자 탈퇴 - 제공업체: {}", provider);
            }
            
            // 회원 정보 삭제
            memberService.deleteMember(authentication.getName());
            
            // 세션 무효화 및 로그아웃 처리
            SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
            logoutHandler.logout(httpRequest, response, authentication);
            
            log.info("회원탈퇴 완료: {}", authentication.getName());
            return ResponseEntity.ok(ApiResponse.success("회원탈퇴가 완료되었습니다. 그동안 이용해주셔서 감사합니다."));
        } catch (IllegalArgumentException e) {
            log.error("회원탈퇴 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("회원탈퇴 중 예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("회원탈퇴 중 오류가 발생했습니다."));
        }
    }
      /**
     * 로그아웃 완료 페이지 - 소셜 로그인 콜백 처리 (개선된 버전)
     */
    @GetMapping("/logout-complete")
    public String logoutComplete(
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String from,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        log.info("🏁 개선된 로그아웃 완료 페이지 접근");
        log.info("  - 제공자: {}", provider);
        log.info("  - 출처: {}", from);
        log.info("  - 요청 IP: {}", request.getRemoteAddr());
        log.info("  - 서버 호스트: {}", request.getServerName());
        
        // 세션이 남아있다면 안전하게 정리
        HttpSession session = request.getSession(false);
        if (session != null) {
            log.info("🧹 남은 세션 안전하게 정리: {}", session.getId());
            try {
                session.invalidate();
            } catch (Exception e) {
                log.warn("⚠️ 세션 정리 중 오류: {}", e.getMessage());
            }
        }
        
        // 추가 쿠키 정리 (안전한 방식)
        try {
            deleteAllCookies(response, request);
            log.info("✅ 쿠키 최종 정리 완료");
        } catch (Exception e) {
            log.warn("⚠️ 최종 쿠키 정리 중 오류: {}", e.getMessage());
        }
        
        log.info("🎯 Thymeleaf 템플릿으로 로그아웃 완료 페이지 렌더링");
        
        // Thymeleaf 템플릿 사용 (파라미터는 자동으로 전달됨)
        return "logout-complete";
    }
}
