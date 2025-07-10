package com.agentica.user.oauth2.provider;

import com.agentica.user.dto.oauth2.OAuth2UserInfo;
import java.util.Map;

public class KakaoOAuth2UserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attributes;

    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getProvider() {
        return "kakao";
    }    @Override
    @SuppressWarnings("unchecked")
    public String getEmail() {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        if (account == null) {
            return null;
        }
        
        // 이메일 제공 동의 여부 확인
        Boolean emailNeedsAgreement = (Boolean) account.get("email_needs_agreement");
        Boolean isEmailValid = (Boolean) account.get("is_email_valid");
        Boolean isEmailVerified = (Boolean) account.get("is_email_verified");
        
        // 이메일이 유효하고 검증된 경우에만 반환
        if (Boolean.TRUE.equals(isEmailValid) && Boolean.TRUE.equals(isEmailVerified) && 
            !Boolean.TRUE.equals(emailNeedsAgreement)) {
            return (String) account.get("email");
        }
        
        // 이메일 정보가 없거나 동의하지 않은 경우 null 반환
        // 서비스 레이어에서 별도 처리 필요
        return null;
    }@Override
    @SuppressWarnings("unchecked")
    public String getName() {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        if (account == null) {
            return "카카오 사용자";
        }
        
        Map<String, Object> profile = (Map<String, Object>) account.get("profile");
        if (profile == null) {
            return "카카오 사용자";
        }
        
        // 닉네임 제공 동의 여부 확인
        Boolean nicknameNeedsAgreement = (Boolean) profile.get("nickname_needs_agreement");
        if (Boolean.TRUE.equals(nicknameNeedsAgreement)) {
            return "카카오 사용자";
        }
        
        String nickname = (String) profile.get("nickname");
        return nickname != null ? nickname : "카카오 사용자";
    }    @Override
    @SuppressWarnings("unchecked")
    public String getProfileImage() {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        if (account == null) {
            return null;
        }
        
        Map<String, Object> profile = (Map<String, Object>) account.get("profile");
        if (profile == null) {
            return null;
        }
        
        // 프로필 이미지 제공 동의 여부 확인
        Boolean profileImageNeedsAgreement = (Boolean) profile.get("profile_image_needs_agreement");
        if (Boolean.TRUE.equals(profileImageNeedsAgreement)) {
            return null;
        }
        
        return (String) profile.get("profile_image_url");
    }
}
