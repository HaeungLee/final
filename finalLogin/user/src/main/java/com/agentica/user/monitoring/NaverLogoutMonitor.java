package com.agentica.user.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 네이버 로그아웃 모니터링 및 디버깅 도구
 */
@Slf4j
@Component
public class NaverLogoutMonitor {

    /**
     * 현재 세션의 네이버 관련 정보를 모니터링
     */
    public Map<String, Object> monitorNaverSession() {
        Map<String, Object> sessionInfo = new HashMap<>();
        
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                
                // 1. 네이버 쿠키 상태 확인
                sessionInfo.put("naverCookies", checkNaverCookies(request));
                
                // 2. 세션 정보 확인
                sessionInfo.put("sessionInfo", checkSessionInfo(request));
                
                // 3. 헤더 정보 확인
                sessionInfo.put("headers", checkRelevantHeaders(request));
                
                log.info("🔍 네이버 세션 모니터링 결과: {}", sessionInfo);
            }
            
        } catch (Exception e) {
            log.error("❌ 네이버 세션 모니터링 중 오류", e);
            sessionInfo.put("error", e.getMessage());
        }
        
        return sessionInfo;
    }
    
    /**
     * 네이버 관련 쿠키 확인
     */
    private Map<String, String> checkNaverCookies(HttpServletRequest request) {
        Map<String, String> naverCookies = new HashMap<>();
        
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                String name = cookie.getName();
                
                // 네이버 관련 쿠키들 체크
                if (name.startsWith("NID_") || 
                    name.contains("NAVER") || 
                    name.contains("naver") ||
                    name.equals("npic")) {
                    
                    // 보안상 값은 마스킹
                    String value = cookie.getValue();
                    String maskedValue = value != null && value.length() > 8 
                        ? value.substring(0, 4) + "****" + value.substring(value.length() - 4)
                        : "****";
                    
                    naverCookies.put(name, maskedValue);
                    log.info("🍪 네이버 쿠키 발견: {} = {} (domain: {}, path: {})", 
                        name, maskedValue, cookie.getDomain(), cookie.getPath());
                }
            }
        }
        
        return naverCookies;
    }
    
    /**
     * 세션 정보 확인
     */
    private Map<String, Object> checkSessionInfo(HttpServletRequest request) {
        Map<String, Object> sessionInfo = new HashMap<>();
        
        try {
            var session = request.getSession(false);
            if (session != null) {
                sessionInfo.put("sessionId", session.getId());
                sessionInfo.put("creationTime", new Date(session.getCreationTime()));
                sessionInfo.put("lastAccessedTime", new Date(session.getLastAccessedTime()));
                sessionInfo.put("maxInactiveInterval", session.getMaxInactiveInterval());
                
                // 세션 속성에서 OAuth2 관련 정보 확인
                Enumeration<String> attributeNames = session.getAttributeNames();
                List<String> oauthAttributes = new ArrayList<>();
                
                while (attributeNames.hasMoreElements()) {
                    String attrName = attributeNames.nextElement();
                    if (attrName.toLowerCase().contains("oauth") || 
                        attrName.toLowerCase().contains("security") ||
                        attrName.toLowerCase().contains("naver")) {
                        oauthAttributes.add(attrName);
                    }
                }
                
                sessionInfo.put("oauthAttributes", oauthAttributes);
            } else {
                sessionInfo.put("status", "no_session");
            }
            
        } catch (Exception e) {
            log.warn("⚠️ 세션 정보 확인 중 오류", e);
            sessionInfo.put("error", e.getMessage());
        }
        
        return sessionInfo;
    }
    
    /**
     * 관련 헤더 확인
     */
    private Map<String, String> checkRelevantHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        
        String[] relevantHeaders = {
            "User-Agent", "Referer", "Origin", "Host",
            "Authorization", "X-Requested-With", "Accept"
        };
        
        for (String headerName : relevantHeaders) {
            String headerValue = request.getHeader(headerName);
            if (headerValue != null) {
                headers.put(headerName, headerValue);
            }
        }
        
        return headers;
    }
    
    /**
     * 로그아웃 후 정리 상태 검증
     */
    public boolean validateLogoutCleanup() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                
                // 네이버 쿠키가 남아있는지 확인
                Map<String, String> remainingNaverCookies = checkNaverCookies(request);
                
                if (!remainingNaverCookies.isEmpty()) {
                    log.warn("⚠️ 로그아웃 후에도 네이버 쿠키가 남아있음: {}", remainingNaverCookies.keySet());
                    return false;
                } else {
                    log.info("✅ 네이버 쿠키 정리 완료 확인");
                    return true;
                }
            }
            
        } catch (Exception e) {
            log.error("❌ 로그아웃 정리 상태 검증 중 오류", e);
        }
        
        return false;
    }
    
    /**
     * 로그아웃 성공률 통계 (간단한 메모리 기반)
     */
    private static final Map<String, Integer> LOGOUT_STATS = new HashMap<>();
    
    public void recordLogoutAttempt(String provider, boolean success) {
        String key = provider + "_" + (success ? "success" : "failure");
        LOGOUT_STATS.merge(key, 1, Integer::sum);
        
        log.info("📊 로그아웃 통계 업데이트: {} (전체 통계: {})", key, LOGOUT_STATS);
    }
    
    public Map<String, Integer> getLogoutStats() {
        return new HashMap<>(LOGOUT_STATS);
    }
}
