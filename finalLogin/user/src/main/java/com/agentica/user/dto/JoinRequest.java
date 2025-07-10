package com.agentica.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinRequest {
    @Email
    private String email;
    
    @NotBlank
    private String password;
    
    @NotBlank
    private String name;
    
    @NotBlank(message = "인증번호는 필수입니다.")
    @Pattern(regexp = "^[0-9]{6}$", message = "인증번호는 6자리 숫자여야 합니다.")
    private String verificationCode;

    public String toSafeLog() {
        return String.format("JoinRequest(email=%s, name=%s, verificationCode=%s)", email, name, verificationCode);
    }
}
