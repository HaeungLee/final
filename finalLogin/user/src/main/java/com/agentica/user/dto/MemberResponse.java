package com.agentica.user.dto;

import com.agentica.user.domain.member.AuthProvider;
import com.agentica.user.domain.member.Member;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberResponse {
    private Long id;
    private String email;
    private String name;
    private String profileImage;
    private AuthProvider provider;
    private boolean canUpdate;

    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
                .id(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .profileImage(member.getProfileImage())
                .provider(member.getProvider())
                .canUpdate(member.canUpdate())
                .build();
    }
}
