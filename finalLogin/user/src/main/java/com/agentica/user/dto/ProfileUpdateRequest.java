package com.agentica.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProfileUpdateRequest {
    
    @NotBlank(message = "이름을 입력해주세요.")
    @Size(min = 2, max = 20, message = "이름은 2자 이상 20자 이하로 입력해주세요.")
    private String name;
    
    @NotBlank(message = "현재 비밀번호를 입력해주세요.")
    private String currentPassword;
    
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    private String password; // 선택적 필드 (비밀번호 변경하지 않을 수 있음)
}
