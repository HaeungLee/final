package com.agentica.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String token = resolveToken(request);
        
        if (token != null && jwtTokenProvider.validateToken(token)) {
            try {
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (UsernameNotFoundException e) {
                // 삭제된 사용자의 토큰인 경우 - 우아하게 처리
                log.warn("삭제된 사용자의 토큰으로 접근 시도: {}", e.getMessage());
                
                // 토큰 관련 쿠키 삭제
                clearAuthCookies(response);
                
                // 인증 컨텍스트 클리어
                SecurityContextHolder.clearContext();
                
                // 로그인 페이지로 리다이렉트 (AJAX 요청이 아닌 경우)
                if (!isAjaxRequest(request)) {
                    response.sendRedirect("/login?error=user_deleted");
                    return;
                }
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * 인증 관련 쿠키 삭제
     */
    private void clearAuthCookies(HttpServletResponse response) {
        String[] cookieNames = {"accessToken", "refreshToken", "JSESSIONID"};
        
        for (String cookieName : cookieNames) {
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(cookieName, "");
            cookie.setPath("/");
            cookie.setMaxAge(0);
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
        }
        
        log.info("인증 쿠키 삭제 완료");
    }
    
    /**
     * AJAX 요청인지 확인
     */
    private boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With")) ||
               request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json");
    }private String resolveToken(HttpServletRequest request) {
        // 먼저 Authorization 헤더에서 토큰 확인
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // Authorization 헤더에 토큰이 없으면 쿠키에서 확인
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }
}
