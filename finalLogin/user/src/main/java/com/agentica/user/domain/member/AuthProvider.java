package com.agentica.user.domain.member;

public enum AuthProvider {
    LOCAL, GOOGLE, NAVER, KAKAO;

    public static AuthProvider fromString(String provider) {
        if (provider == null) {
            return LOCAL;
        }
        
        switch (provider.toLowerCase()) {
            case "google":
                return GOOGLE;
            case "naver":
                return NAVER;
            case "kakao":
                return KAKAO;
            default:
                return LOCAL;
        }
    }
}
