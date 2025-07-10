package com.agentica.user.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication) throws IOException, ServletException {
        
        log.info("=== CustomLogoutSuccessHandler 실행 ===");

        if (authentication != null) {
            log.info("로그아웃된 사용자: {}", authentication.getName());
        }

        // ✅ 모든 관련 쿠키 강력 삭제 (네이버 특화 쿠키 포함)
        String[] commonCookieNames = {
            "JSESSIONID", "refreshToken", "refresh_token", "accessToken", "access_token",
            "PGADMIN_LANGUAGE", "pgadmin_session", "jwt_token", "auth_token",
            // 네이버 특화 쿠키 추가
            "NID_AUT", "NID_SES", "NID_JKL", "NAVER_OPEN_RCVR", "npic",
            // 구글 특화 쿠키
            "SAPISID", "SSID", "HSID", "SID", "APISID", "1P_JAR",
            // 카카오 특화 쿠키
            "_kadu", "_kadub", "_karmt", "_karmtb"
        };
        
        // 미리 정의된 쿠키들 삭제
        for (String cookieName : commonCookieNames) {
            deleteCookieAllPaths(response, cookieName);
        }
        
        // 요청에서 발견된 모든 쿠키 삭제
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                log.info("발견된 쿠키: {} = {}", cookie.getName(), cookie.getValue());
                deleteCookieAllPaths(response, cookie.getName());
            }
        }

        // 요청이 AJAX인지 확인
        String requestedWith = request.getHeader("X-Requested-With");
        boolean isAjax = "XMLHttpRequest".equals(requestedWith);

        // Accept 헤더 확인
        String acceptHeader = request.getHeader("Accept");
        boolean isJsonRequest = acceptHeader != null && acceptHeader.contains("application/json");

        if (isAjax || isJsonRequest) {
            // ✅ JSON 응답 (AJAX 요청 대응)
            log.info("AJAX 로그아웃 요청 - JSON 응답 반환");
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"success\":true,\"message\":\"로그아웃이 완료되었습니다.\",\"redirectUrl\":\"/\"}");
        } else {
            // ✅ 일반 폼 요청 대응 - 리다이렉트
            log.info("일반 로그아웃 요청 - 메인 페이지로 리다이렉트");
            response.sendRedirect("/?logout=true");
        }

        log.info("=== 로그아웃 처리 완료 ===");
    }

    /**
     * 모든 경로에서 쿠키 삭제 (더 강력한 삭제)
     */
    private void deleteCookieAllPaths(HttpServletResponse response, String cookieName) {
        // 다양한 경로에서 쿠키 삭제 시도
        String[] paths = {"/", "/user", "/api", ""};
        String[] domains = {"", ".naver.com", ".google.com", ".kakao.com"};
        
        for (String path : paths) {
            for (String domain : domains) {
                // 개발환경용: Secure=false 쿠키 삭제
                Cookie cookie = new Cookie(cookieName, "");
                cookie.setPath(path);
                cookie.setMaxAge(0);
                cookie.setHttpOnly(true);
                cookie.setSecure(false); // 개발환경 설정과 일치
                if (!domain.isEmpty()) {
                    cookie.setDomain(domain);
                }
                response.addCookie(cookie);
            }
        }
        log.info("강력 쿠키 삭제 완료: {}", cookieName);
    }

    /**
     * 쿠키 삭제 유틸리티 메서드
     */
    private void deleteCookie(HttpServletResponse response, String cookieName, String path) {
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setPath(path);
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);  // 개발환경 설정과 일치
        response.addCookie(cookie);
        log.info("쿠키 삭제: {}", cookieName);
    }
}
