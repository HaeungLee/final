package com.agentica.user.security;

import com.agentica.user.service.SocialLogoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimpleLogoutSuccessHandler implements LogoutSuccessHandler {

    private final SocialLogoutService socialLogoutService;
    private final OAuth2AuthorizedClientService authorizedClientService;    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication) throws IOException, ServletException {
        
        log.info("=== 로그아웃 성공 핸들러 실행 ===");
          try {
            // 1. 세션 강제 무효화 (우선 처리)
            forceInvalidateSession(request);
            
            // 2. OAuth2 토큰 정리
            if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
                String registrationId = oauth2Token.getAuthorizedClientRegistrationId();
                String principalName = authentication.getName();
                
                log.info("OAuth2 토큰 정리: {}", registrationId);
                authorizedClientService.removeAuthorizedClient(registrationId, principalName);
            }
            
            // 3. 소셜 로그아웃 처리
            String userEmail = authentication != null ? authentication.getName() : null;
            socialLogoutService.processLogout(authentication, userEmail);
            
            // 4. 강화된 쿠키 정리 (세션 무효화 후 실행)
            deleteAllCookies(request, response);
            
            // 4. 소셜 로그아웃 URL 처리
            var provider = socialLogoutService.getProvider(authentication);
            boolean isJson = isJsonRequest(request);            if (socialLogoutService.needsSocialLogout(provider)) {
                String logoutUrl = socialLogoutService.getSocialLogoutUrl(provider);
                
                log.info("소셜 로그아웃 처리: {} -> {}", provider, logoutUrl);
                
                if (isJson) {
                    writeJsonResponse(response, String.format(
                        "{\"success\":true,\"message\":\"소셜 로그아웃 중\",\"socialLogoutUrl\":\"%s\",\"provider\":\"%s\"}",
                        logoutUrl, provider
                    ));
                } else {
                    // 통합 개선된 소셜 로그아웃 처리 페이지로 이동
                    if (logoutUrl != null && !logoutUrl.isEmpty()) {
                        log.info("개선된 소셜 로그아웃 페이지로 리다이렉트: {}", logoutUrl);
                        response.sendRedirect("/logout-social?provider=" + provider + "&socialLogoutUrl=" + 
                                            java.net.URLEncoder.encode(logoutUrl, java.nio.charset.StandardCharsets.UTF_8));
                    } else {
                        log.warn("소셜 로그아웃 URL이 없습니다. 일반 로그아웃 완료 처리");
                        response.sendRedirect("/logout-complete");
                    }
                }
                return;
            }

            // 5. 일반 로그아웃 완료
            log.info("일반 로그아웃 처리 완료");
            if (isJson) {
                writeJsonResponse(response, "{\"success\":true,\"message\":\"로그아웃이 완료되었습니다.\",\"redirectUrl\":\"/logout-complete\"}");
            } else {
                response.sendRedirect("/logout-complete");
            }
            
        } catch (Exception e) {
            log.error("로그아웃 처리 중 오류 발생", e);
            if (!response.isCommitted()) {
                response.sendRedirect("/?logout=true&error=true");
            }
        }
        
        log.info("=== 로그아웃 처리 완료 ===");
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    private void writeJsonResponse(HttpServletResponse response, String json) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(json);
        response.getWriter().flush();
    }

    private void deleteAllCookies(HttpServletRequest request, HttpServletResponse response) {
        try {
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    deleteCookie(response, cookie.getName());
                }
            }            // 추가적으로 삭제할 쿠키들 (네이버 특화 포함)
            String[] extraCookies = {
                "JSESSIONID", "refreshToken", "accessToken", "PGLADMIN_LANGUAGE",
                "oauth2_auth_request", "oauth2_redirect_uri", 
                "SPRING_SECURITY_REMEMBER_ME_COOKIE", "remember-me",
                "AUTH-TOKEN", "XSRF-TOKEN", "_oauth2_authorization_requests",
                // 네이버 특화 쿠키 추가
                "NID_AUT", "NID_JKL", "NID_SES", "NID_JKL",
                "naver_login_location", "naver_enctp", "naver_locale",
                // 추가 소셜 로그인 관련 쿠키
                "oauth_token", "oauth_token_secret", "state", "code_verifier", 
                "naver_state", "google_state", "kakao_state",
                // JWT 관련 추가 쿠키
                "jwt_token", "auth_token", "bearer_token"
            };
            for (String name : extraCookies) {
                deleteCookie(response, name);
            }

        } catch (Exception e) {
            log.error("쿠키 삭제 중 오류", e);
        }
    }    private void deleteCookie(HttpServletResponse response, String name) {
        log.info("🎯 쿠키 삭제 시작: {}", name);
        
        // 특별 처리가 필요한 쿠키들
        if ("JSESSIONID".equals(name) || "accessToken".equals(name) || "refreshToken".equals(name)) {
            deleteSpecialCookie(response, name);
            return;
        }
        
        // 일반 쿠키 삭제
        deleteGeneralCookie(response, name);
    }
      /**
     * 특별 쿠키 삭제 (JSESSIONID, accessToken, refreshToken) - 정확한 속성 매칭
     */
    private void deleteSpecialCookie(HttpServletResponse response, String name) {
        log.info("🔥 정확한 속성 매칭으로 특별 쿠키 삭제 시작: {}", name);
        
        try {
            // 1. 실제 쿠키 생성 속성과 정확히 일치하도록 삭제
            if ("accessToken".equals(name) || "refreshToken".equals(name)) {
                // OAuth2SuccessHandler에서 생성한 속성과 정확히 일치
                Cookie cookie = new Cookie(name, "");
                cookie.setHttpOnly(true);   // 생성 시와 동일
                cookie.setSecure(false);    // 개발환경 설정과 동일
                cookie.setPath("/");        // 생성 시와 동일
                cookie.setMaxAge(0);        // 삭제를 위해 0으로 설정
                response.addCookie(cookie);
                
                log.info("✅ {} 쿠키 삭제 (OAuth2 속성 일치): HttpOnly=true, Secure=false, Path=/", name);
            }
            
            if ("JSESSIONID".equals(name)) {
                // JSESSIONID는 Spring에서 자동 생성되므로 다양한 조합 시도
                String[] paths = {"/", ""};
                boolean[] httpOnlyOptions = {true, false};
                
                for (String path : paths) {
                    for (boolean httpOnly : httpOnlyOptions) {
                        Cookie cookie = new Cookie(name, "");
                        cookie.setHttpOnly(httpOnly);
                        cookie.setSecure(false);    // 개발환경
                        cookie.setPath(path);
                        cookie.setMaxAge(0);
                        response.addCookie(cookie);
                        
                        log.debug("JSESSIONID 삭제 시도: HttpOnly={}, Path={}", httpOnly, path);
                    }
                }
                
                log.info("✅ JSESSIONID 쿠키 삭제 완료");
            }
            
            // 2. 추가 보장을 위한 Set-Cookie 헤더 직접 설정
            response.addHeader("Set-Cookie", 
                String.format("%s=; Path=/; Max-Age=0; HttpOnly; Expires=Thu, 01 Jan 1970 00:00:00 GMT", name));
            
            log.info("✅ 특별 쿠키 삭제 완료: {} (속성 정확히 매칭)", name);
            
        } catch (Exception e) {
            log.error("❌ 특별 쿠키 삭제 중 오류: {}", name, e);
        }
    }
    
    /**
     * 일반 쿠키 삭제
     */
    private void deleteGeneralCookie(HttpServletResponse response, String name) {
        try {
            Cookie cookie = new Cookie(name, "");
            cookie.setPath("/");
            cookie.setMaxAge(0);
            cookie.setHttpOnly(true);
            cookie.setSecure(false);
            response.addCookie(cookie);
            
            // 추가 경로에서도 삭제 시도
            String[] paths = {"/", "/api", ""};
            for (String path : paths) {
                Cookie pathCookie = new Cookie(name, "");
                pathCookie.setPath(path);
                pathCookie.setMaxAge(0);
                pathCookie.setHttpOnly(true);
                pathCookie.setSecure(false);
                response.addCookie(pathCookie);
            }
            
            log.info("✅ 일반 쿠키 삭제 완료: {}", name);
        } catch (Exception e) {
            log.error("❌ 일반 쿠키 삭제 중 오류: {}", name, e);
        }    }

    /**
     * 강제 세션 무효화 (JSESSIONID 삭제 보장)
     */
    private void forceInvalidateSession(HttpServletRequest request) {
        log.info("🔥 강제 세션 무효화 시작");
        
        try {
            // 1. 현재 세션 가져오기 (있다면)
            HttpSession session = request.getSession(false);
            if (session != null) {
                String sessionId = session.getId();
                log.info("📍 기존 세션 발견: {}", sessionId);
                
                // 2. 세션 무효화
                session.invalidate();
                log.info("✅ 세션 무효화 완료: {}", sessionId);
            } else {
                log.info("ℹ️ 기존 세션 없음");
            }
            
            // 3. SecurityContext 정리
            SecurityContextHolder.clearContext();
            log.info("✅ SecurityContext 정리 완료");
            
        } catch (Exception e) {
            log.error("❌ 강제 세션 무효화 중 오류", e);
        }
        
        log.info("🔥 강제 세션 무효화 완료");
    }

}
