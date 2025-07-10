package com.agentica.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetRequest {
    @Email
    private String email;
    
    @NotBlank
    private String token;
    
    @NotBlank
    private String newPassword;
}
