package com.agentica.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원탈퇴 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DeleteAccountRequest {
    
    @NotBlank(message = "현재 비밀번호를 입력해주세요.")
    private String currentPassword;
}
