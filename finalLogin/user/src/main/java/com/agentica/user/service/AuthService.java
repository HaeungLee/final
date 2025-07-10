package com.agentica.user.service;

import com.agentica.user.domain.member.AuthProvider;
import com.agentica.user.domain.member.Member;
import com.agentica.user.domain.member.MemberRepository;
import com.agentica.user.domain.member.Role;
import com.agentica.user.dto.JoinRequest;
import com.agentica.user.dto.LoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;

/**
 * 인증 및 회원가입 처리 전담 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    
    private final MemberRepository memberRepository;
    private final VerificationService verificationService;
    private final @Lazy PasswordEncoder passwordEncoder;
    private final @Lazy AuthenticationManager authenticationManager;
    private final DataSource dataSource;
    /**
     * 회원가입
     */
    public void join(JoinRequest joinRequest) {
        String email = joinRequest.getEmail();
        String verificationCode = joinRequest.getVerificationCode();
        
        System.out.println("=== 회원가입 처리 ===");
        System.out.println("이메일: " + email);
        System.out.println("인증번호: " + verificationCode);
        

         try {
            System.out.println("📎 현재 DB 연결 정보: " + dataSource.getConnection().getMetaData().getURL());
        } catch (SQLException e) {
            System.err.println("❌ DB 연결 정보 출력 중 오류: " + e.getMessage());
        }

        // 이미 가입된 회원인지 확인
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        
        // 인증번호 검증
        if (!verificationService.isEmailVerified(email, verificationCode)) {
            throw new IllegalArgumentException("유효하지 않은 인증번호입니다.");
        }
        
        System.out.println("인증번호 검증 성공!");
        
        // 회원 생성
        Member member = Member.builder()
                .email(joinRequest.getEmail())
                .password(passwordEncoder.encode(joinRequest.getPassword()))
                .name(joinRequest.getName())
                .role(Role.USER)
                .provider(AuthProvider.LOCAL)
                .emailVerified(true) // 인증번호로 이미 확인했으므로 true
                .build();

        memberRepository.save(member);
        
        // 사용된 인증번호 삭제
        verificationService.cleanupVerificationData(email);
        
        System.out.println("회원가입 완료!");
    }
    
    /**
     * 로그인
     */
    public Authentication login(LoginRequest loginRequest) {
        Member member = memberRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));

        if (!member.isEmailVerified()) {
            throw new IllegalArgumentException("이메일 인증이 필요합니다.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }
    
    /**
     * 이메일 중복 확인
     */
    @Transactional(readOnly = true)
    public boolean isEmailExists(String email) {
        return memberRepository.existsByEmail(email);
    }
}
