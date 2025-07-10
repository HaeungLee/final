package com.agentica.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class DeleteAccountTokenService {
    
    private final Map<String, DeleteAccountToken> tokenStore = new ConcurrentHashMap<>();
    private static final int TOKEN_EXPIRY_MINUTES = 5;
    
    /**
     * 회원탈퇴용 임시 토큰 생성
     */
    public String createDeleteToken(String email) {
        log.info("회원탈퇴 임시 토큰 생성: {}", email);
        
        // 기존 토큰 제거 (같은 이메일)
        tokenStore.entrySet().removeIf(entry -> email.equals(entry.getValue().getEmail()));
        
        // 새 토큰 생성
        String token = UUID.randomUUID().toString();
        DeleteAccountToken deleteToken = new DeleteAccountToken(
            email, 
            LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES)
        );
        
        tokenStore.put(token, deleteToken);
        
        // 만료된 토큰 정리
        cleanExpiredTokens();
        
        log.info("회원탈퇴 임시 토큰 생성 완료: {} (만료시간: {})", 
            token.substring(0, 8) + "...", deleteToken.getExpiryTime());
        
        return token;
    }
    
    /**
     * 토큰 검증 및 이메일 반환
     */
    public String validateAndGetEmail(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("유효하지 않은 토큰: null 또는 빈 문자열");
            return null;
        }
        
        DeleteAccountToken deleteToken = tokenStore.get(token);
        if (deleteToken == null) {
            log.warn("존재하지 않는 토큰: {}", token.substring(0, Math.min(8, token.length())) + "...");
            return null;
        }
        
        if (deleteToken.isExpired()) {
            log.warn("만료된 토큰: {} (만료시간: {})", 
                token.substring(0, 8) + "...", deleteToken.getExpiryTime());
            tokenStore.remove(token);
            return null;
        }
        
        log.info("토큰 검증 성공: {} -> {}", 
            token.substring(0, 8) + "...", deleteToken.getEmail());
        
        return deleteToken.getEmail();
    }
    
    /**
     * 토큰 제거
     */
    public void removeToken(String token) {
        if (token != null) {
            tokenStore.remove(token);
            log.info("토큰 제거 완료: {}", token.substring(0, Math.min(8, token.length())) + "...");
        }
    }
    
    /**
     * 만료된 토큰 정리
     */
    private void cleanExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        int removedCount = 0;
        
        var iterator = tokenStore.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().getExpiryTime().isBefore(now)) {
                iterator.remove();
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            log.info("만료된 토큰 {} 개 정리 완료", removedCount);
        }
    }
    
    /**
     * 내부 토큰 클래스
     */
    private static class DeleteAccountToken {
        private final String email;
        private final LocalDateTime expiryTime;
        
        public DeleteAccountToken(String email, LocalDateTime expiryTime) {
            this.email = email;
            this.expiryTime = expiryTime;
        }
        
        public String getEmail() {
            return email;
        }
        
        public LocalDateTime getExpiryTime() {
            return expiryTime;
        }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }
    }
}
