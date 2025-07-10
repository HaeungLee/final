package com.agentica.user.domain.member;

import com.agentica.user.dto.oauth2.OAuth2UserInfo;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    @Column(nullable = false)
    private String name;    private String profileImage;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }    @Builder
    public Member(String email, String password, String name, String profileImage, Role role, AuthProvider provider, boolean emailVerified) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.profileImage = profileImage;
        this.role = role;
        this.provider = provider != null ? provider : AuthProvider.LOCAL;
        this.emailVerified = emailVerified;
    }

    public Member update(String name, String profileImage) {
        this.name = name;
        this.profileImage = profileImage;
        return this;
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }    public void updatePassword(String password) {
        this.password = password;
    }

    /**
     * 소셜 로그인 사용자가 비밀번호를 설정하여 일반 사용자로 전환
     */
    public void convertToLocalUser(String password) {
        this.password = password;
        this.provider = AuthProvider.LOCAL;
    }

    public boolean canUpdate() {
        return this.provider == AuthProvider.LOCAL;
    }
    
    /**
     * 소셜 로그인 사용자인지 확인
     */
    public boolean isSocialUser() {
        return this.provider != AuthProvider.LOCAL;
    }
    
    /**
     * 비밀번호가 설정되어 있는지 확인
     */
    public boolean hasPassword() {
        return this.password != null && !this.password.trim().isEmpty();
    }public void checkCanUpdate() {
        if (!canUpdate()) {
            throw new com.agentica.user.exception.SocialLoginUserException("소셜 로그인 사용자는 정보 수정이 불가능합니다.");
        }
    }public static Member createOAuth2Member(OAuth2UserInfo userInfo) {
        return Member.builder()
                .email(userInfo.getEmail())
                .name(userInfo.getName())
                .profileImage(userInfo.getProfileImage())
                .role(Role.USER)
                .provider(AuthProvider.fromString(userInfo.getProvider()))
                .emailVerified(true) // OAuth2 로그인은 이메일 인증됨으로 처리
                .build();
    }
}
