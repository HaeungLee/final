package com.agentica.user.dto.oauth2;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OAuth2LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private String email;
    private String name;
    private String profileImage;
    
    public static OAuth2LoginResponse of(String accessToken, String refreshToken, Long expiresIn, 
                                        String email, String name, String profileImage) {
        return new OAuth2LoginResponse(accessToken, refreshToken, "Bearer", expiresIn, 
                                     email, name, profileImage);
    }
}
