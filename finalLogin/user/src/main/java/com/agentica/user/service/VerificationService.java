package com.agentica.user.service;

import com.agentica.user.domain.verification.EmailVerificationCode;
import com.agentica.user.domain.verification.EmailVerificationCodeRepository;
import com.agentica.user.dto.EmailVerificationRequest;
import com.agentica.user.dto.VerifyCodeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * 이메일 인증 처리 전담 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class VerificationService {
    
    private final EmailVerificationCodeRepository emailVerificationCodeRepository;
    private final EmailService emailService;
    
    /**
     * 이메일 인증번호 전송
     */
    public void sendVerificationCode(EmailVerificationRequest request) {
        String email = request.getEmail();
        
        System.out.println("=== 인증번호 전송 요청 ===");
        System.out.println("요청 이메일: " + email);
        
        // 기존 인증번호 삭제
        emailVerificationCodeRepository.deleteByEmail(email);
        
        // 6자리 인증번호 생성
        String verificationCode = generateVerificationCode();
        
        // 인증번호 저장 (3분 유효)
        EmailVerificationCode emailVerificationCode = EmailVerificationCode.builder()
                .email(email)
                .code(verificationCode)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(3))
                .build();
        
        emailVerificationCodeRepository.save(emailVerificationCode);
        
        // 이메일 전송
        emailService.sendVerificationCodeEmail(email, verificationCode);
        
        System.out.println("=== 인증번호 발송 ===");
        System.out.println("이메일: " + email);
        System.out.println("인증번호: " + verificationCode);
        System.out.println("만료시간: " + emailVerificationCode.getExpiresAt());
    }
    
    /**
     * 인증번호 확인
     */
    public void verifyCode(VerifyCodeRequest request) {
        String email = request.getEmail();
        String code = request.getVerificationCode();
        
        System.out.println("=== 인증번호 확인 ===");
        System.out.println("이메일: " + email);
        System.out.println("입력된 코드: " + code);
        
        // 가장 최근 인증번호 조회
        EmailVerificationCode verificationCode = emailVerificationCodeRepository
                .findByEmailAndVerifiedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new IllegalArgumentException("인증번호를 먼저 요청해주세요."));
        
        // 최대 시도 횟수 확인
        if (verificationCode.isMaxAttemptsReached()) {
            throw new IllegalArgumentException("인증 시도 횟수를 초과했습니다. 새로운 인증번호를 요청해주세요.");
        }
        
        // 만료 확인
        if (verificationCode.isExpired()) {
            throw new IllegalArgumentException("인증번호가 만료되었습니다. 새로운 인증번호를 요청해주세요.");
        }
        
        // 시도 횟수 증가
        verificationCode.incrementAttempt();
        emailVerificationCodeRepository.save(verificationCode);
        
        // 인증번호 확인
        if (!verificationCode.getCode().equals(code)) {
            int remainingAttempts = 5 - verificationCode.getAttemptCount();
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다. (남은 시도: " + remainingAttempts + "번)");
        }
        
        // 인증 완료 처리
        verificationCode.verify();
        emailVerificationCodeRepository.save(verificationCode);
        
        System.out.println("인증 성공!");
    }
    
    /**
     * 회원가입용 인증번호 검증
     */
    public boolean isEmailVerified(String email, String verificationCode) {
        return emailVerificationCodeRepository
                .findByEmailAndCodeAndVerifiedTrue(email, verificationCode)
                .isPresent();
    }
    
    /**
     * 인증 완료된 이메일인지 확인
     */
    public boolean hasVerifiedEmail(String email) {
        return emailVerificationCodeRepository.existsByEmailAndVerifiedTrue(email);
    }
    
    /**
     * 인증 완료 후 데이터 정리
     */
    public void cleanupVerificationData(String email) {
        emailVerificationCodeRepository.deleteByEmail(email);
    }
    
    /**
     * 6자리 랜덤 인증번호 생성
     */
    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        return String.format("%06d", random.nextInt(1000000));
    }
}
