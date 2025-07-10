package com.agentica.user.exception;

public class SocialLoginUserException extends RuntimeException {
    public SocialLoginUserException() {
        super("소셜 로그인 사용자는 해당 기능을 사용할 수 없습니다.");
    }
    
    public SocialLoginUserException(String message) {
        super(message);
    }
}
