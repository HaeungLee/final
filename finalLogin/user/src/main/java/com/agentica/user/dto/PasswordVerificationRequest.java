package com.agentica.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordVerificationRequest {
    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;
}
