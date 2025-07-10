package com.agentica.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 소셜 사용자 비밀번호 설정 요청 DTO
 */
@Getter
@Setter
public class SetPasswordRequest {
    
    @NotBlank(message = "비밀번호를 입력해주세요")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    private String password;
    
    @NotBlank(message = "비밀번호 확인을 입력해주세요")
    private String confirmPassword;
    
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(confirmPassword);
    }
}
