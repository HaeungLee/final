package com.agentica.user.exception;

import com.agentica.user.response.ApiResponse;
import com.agentica.user.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // CustomException 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleCustomException(CustomException e) {
        log.error("CustomException: {}", e.getMessage(), e);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(e.getErrorCode())
                .message(e.getMessage())
                .build();
        
        return ResponseEntity
                .status(e.getStatusCode())
                .body(ApiResponse.error(errorResponse));
    }

    // 유효성 검증 실패 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleValidationException(MethodArgumentNotValidException e) {
        log.error("Validation error: {}", e.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(fieldName, message);
        });
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("VALIDATION_FAILED")
                .message("입력값 검증에 실패했습니다.")
                .details(errors)
                .build();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorResponse));
    }

    // 인증 실패 처리
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleBadCredentialsException(BadCredentialsException e) {
        log.error("Authentication failed: {}", e.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("AUTHENTICATION_FAILED")
                .message("이메일 또는 비밀번호가 올바르지 않습니다.")
                .build();
        
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(errorResponse));
    }

    // 일반적인 IllegalArgumentException 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("IllegalArgumentException: {}", e.getMessage(), e);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("BAD_REQUEST")
                .message(e.getMessage())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)                .body(ApiResponse.error(errorResponse));
    }

    // 소셜 로그인 사용자 예외 처리
    @ExceptionHandler(SocialLoginUserException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleSocialLoginUserException(SocialLoginUserException e) {
        log.error("SocialLoginUserException: {}", e.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("SOCIAL_001")
                .message(e.getMessage())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(errorResponse));
    }

    // 기타 모든 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleGeneralException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("INTERNAL_SERVER_ERROR")
                .message("서버 내부 오류가 발생했습니다.")
                .build();
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorResponse));
    }
}
