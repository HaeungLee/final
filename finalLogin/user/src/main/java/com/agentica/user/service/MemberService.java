package com.agentica.user.service;

import com.agentica.user.domain.member.Member;
import com.agentica.user.domain.member.MemberRepository;
import com.agentica.user.dto.MemberResponse;
import com.agentica.user.dto.ProfileUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 정보 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {
    
    private final MemberRepository memberRepository;
    private final @Lazy PasswordEncoder passwordEncoder;
    
    /**
     * 회원 정보 조회
     */
    @Transactional(readOnly = true)
    public MemberResponse getMemberInfo(String email) {
        Member member = findByEmail(email);
        return MemberResponse.from(member);
    }

    /**
     * 이메일로 회원 찾기
     */
    public Member findByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }

    /**
     * 비밀번호 확인
     */
    public boolean verifyPassword(String email, String password) {
        Member member = findByEmail(email);
        member.checkCanUpdate();
        return passwordEncoder.matches(password, member.getPassword());
    }    /**
     * 프로필 수정
     */
    public void updateProfile(String email, ProfileUpdateRequest request) {
        Member member = findByEmail(email);
        member.checkCanUpdate();
        
        member.update(request.getName(), member.getProfileImage());
        
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            member.updatePassword(passwordEncoder.encode(request.getPassword()));
        }
        memberRepository.save(member);
    }
      /**
     * 소셜 로그인 사용자 비밀번호 설정 (소셜 → 일반 사용자 전환)
     */
    public void setSocialUserPassword(String email, String password) {
        log.info("=== 소셜 사용자 비밀번호 설정 시작 ===");
        log.info("사용자 이메일: {}", email);
        
        Member member = findByEmail(email);
        log.info("회원 조회 완료: provider={}, hasPassword={}", member.getProvider(), member.hasPassword());
        
        if (!member.isSocialUser()) {
            log.warn("이미 일반 사용자임: provider={}", member.getProvider());
            throw new IllegalArgumentException("이미 일반 사용자입니다.");
        }
        
        if (password == null || password.trim().isEmpty()) {
            log.warn("비밀번호가 비어있음");
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }
        
        // 비밀번호 강도 검증 (영문, 숫자, 특수문자 조합 8자 이상)
        if (!isValidPassword(password)) {
            log.warn("비밀번호 강도 검증 실패: 길이={}", password.length());
            throw new IllegalArgumentException("비밀번호는 영문, 숫자, 특수문자 조합으로 8자 이상이어야 합니다.");
        }
        
        log.info("비밀번호 암호화 및 사용자 전환 시작");
        // 소셜 사용자를 일반 사용자로 전환
        member.convertToLocalUser(passwordEncoder.encode(password));
        Member savedMember = memberRepository.save(member);
        
        log.info("소셜 → 일반 사용자 전환 완료: {} → {}", member.getProvider(), savedMember.getProvider());    }
    
    /**
     * 회원 탈퇴 (완전 삭제)
     */
    public void deleteMember(String email) {
        log.info("=== 회원탈퇴 시작 ===");
        log.info("탈퇴 대상 이메일: {}", email);
        
        Member member = findByEmail(email);
        log.info("탈퇴 대상 회원 정보: id={}, name={}, provider={}", 
                member.getId(), member.getName(), member.getProvider());
        
        // 회원 정보 DB에서 완전 삭제
        memberRepository.delete(member);
        
        log.info("회원탈퇴 완료 - DB에서 완전 삭제됨: {}", email);
    }
    
    /**
     * 비밀번호 강도 검증
     */
    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
        
        return hasLetter && hasDigit && hasSpecial;
    }
}
