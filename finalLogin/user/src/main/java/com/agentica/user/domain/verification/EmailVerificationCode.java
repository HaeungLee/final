package com.agentica.user.domain.verification;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification_code")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationCode {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false, length = 6)
    private String code;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(nullable = false)
    @Builder.Default
    private boolean verified = false;
    
    @Column(nullable = false)
    @Builder.Default
    private int attemptCount = 0;
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isVerified() {
        return verified;
    }
    
    public void verify() {
        this.verified = true;
    }
    
    public void incrementAttempt() {
        this.attemptCount++;
    }
    
    public boolean isMaxAttemptsReached() {
        return attemptCount >= 5; // 최대 5번 시도
    }
}
