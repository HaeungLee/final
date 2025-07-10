package com.agentica.user.domain.verification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {
      Optional<EmailVerificationCode> findByEmailAndCodeAndVerifiedFalse(String email, String code);
    
    Optional<EmailVerificationCode> findByEmailAndCodeAndVerifiedTrue(String email, String code);
    
    Optional<EmailVerificationCode> findByEmailAndVerifiedFalseOrderByCreatedAtDesc(String email);
    
    // 인증 완료된 이메일인지 확인 (회원가입용)
    boolean existsByEmailAndVerifiedTrue(String email);
    
    void deleteByEmail(String email);
    
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
