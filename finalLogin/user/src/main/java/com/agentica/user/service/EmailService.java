package com.agentica.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    // 인증번호 이메일 전송
    public void sendVerificationCodeEmail(String toEmail, String verificationCode) {
        try {
            // 콘솔에도 출력 (디버깅용)
            System.out.println("==============================================");
            System.out.println("📧 인증번호 이메일 전송");
            System.out.println("==============================================");
            System.out.println("받는 사람: " + toEmail);
            System.out.println("인증번호: " + verificationCode);
            System.out.println("==============================================");
            
            // 실제 이메일 전송
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("이메일 인증번호");

            Context context = new Context();
            context.setVariable("verificationCode", verificationCode);
            context.setVariable("toEmail", toEmail);

            String htmlContent = templateEngine.process("email/verification-code", context);
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            System.out.println("✅ 인증번호 이메일 전송 완료: " + toEmail);
        } catch (MessagingException e) {
            System.err.println("❌ 인증번호 이메일 전송 실패: " + e.getMessage());
            throw new RuntimeException("이메일 전송에 실패했습니다.", e);
        }
    }
}
