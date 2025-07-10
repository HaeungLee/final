package com.agentica.user.service;

import com.agentica.user.domain.member.Member;
import com.agentica.user.domain.member.MemberRepository;
import com.agentica.user.dto.oauth2.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OAuth2 전용 회원 서비스
 * SecurityConfig와의 순환 참조를 피하기 위해 분리된 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OAuth2MemberService {
    
    private final MemberRepository memberRepository;    /**
     * OAuth2 사용자 저장 또는 업데이트
     * @param userInfo OAuth2 사용자 정보
     * @return 저장/업데이트된 회원 정보
     */
    public Member saveOrUpdateOAuth2User(OAuth2UserInfo userInfo) {
        log.info("=== OAuth2 사용자 저장/업데이트 시작 ===");
        log.info("사용자 정보 - Provider: {}, Email: {}, Name: {}, ProviderId: {}", 
                userInfo.getProvider(), userInfo.getEmail(), userInfo.getName(), userInfo.getProviderId());
        
        // 카카오에서 이메일을 제공하지 않는 경우 처리
        if (userInfo.getEmail() == null || userInfo.getEmail().trim().isEmpty()) {
            if ("kakao".equals(userInfo.getProvider())) {
                log.warn("카카오 사용자의 이메일 정보가 없습니다. ProviderId: {}", userInfo.getProviderId());
                // 임시 이메일 생성 (실제 서비스에서는 추가 이메일 입력 프로세스 필요)
                String tempEmail = "kakao_" + userInfo.getProviderId() + "@temp.kakao";
                log.info("임시 이메일 생성: {}", tempEmail);
                // 임시로 이메일을 설정하되, 추후 실제 이메일 입력 받도록 플래그 설정 필요
                return createKakaoMemberWithTempEmail(userInfo, tempEmail);
            } else {
                throw new IllegalArgumentException("이메일 정보가 필요합니다: " + userInfo.getProvider());
            }
        }
        
        try {
            Member member = memberRepository.findByEmail(userInfo.getEmail())
                    .map(existingMember -> {
                        log.info("기존 회원 발견 - ID: {}, Email: {}", existingMember.getId(), existingMember.getEmail());
                        return existingMember.update(userInfo.getName(), userInfo.getProfileImage());
                    })
                    .orElseGet(() -> {
                        log.info("신규 회원 생성 시작");
                        return Member.createOAuth2Member(userInfo);
                    });
            
            Member savedMember = memberRepository.save(member);
            log.info("OAuth2 사용자 저장/업데이트 완료 - ID: {}, Email: {}", savedMember.getId(), savedMember.getEmail());
            
            return savedMember;
        } catch (Exception e) {
            log.error("OAuth2 사용자 저장/업데이트 실패 - Error: {}", e.getMessage(), e);
            throw e;
        }
    }
      /**
     * 카카오 사용자의 임시 이메일로 회원 생성
     * 추후 실제 이메일 입력 받는 프로세스 추가 필요
     */
    private Member createKakaoMemberWithTempEmail(OAuth2UserInfo userInfo, String tempEmail) {
        log.info("카카오 사용자 임시 회원 생성 - TempEmail: {}", tempEmail);
        
        // 임시 이메일로 이미 생성된 회원이 있는지 확인
        return memberRepository.findByEmail(tempEmail)
                .map(existingMember -> {
                    log.info("임시 이메일로 생성된 기존 회원 발견 - ID: {}", existingMember.getId());
                    return existingMember.update(userInfo.getName(), userInfo.getProfileImage());
                })
                .orElseGet(() -> {
                    log.info("임시 이메일로 신규 회원 생성");
                    // 임시 OAuth2UserInfo 생성 (이메일만 변경)
                    OAuth2UserInfo tempUserInfo = new OAuth2UserInfo() {
                        @Override
                        public String getProviderId() { return userInfo.getProviderId(); }
                        @Override
                        public String getProvider() { return userInfo.getProvider(); }
                        @Override
                        public String getEmail() { return tempEmail; }
                        @Override
                        public String getName() { return userInfo.getName(); }
                        @Override
                        public String getProfileImage() { return userInfo.getProfileImage(); }
                    };
                    return Member.createOAuth2Member(tempUserInfo);
                });
    }
}
