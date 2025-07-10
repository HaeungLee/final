package com.agentica.user.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    private final String errorCode;
    private final int statusCode;

    public CustomException(String errorCode, String message, int statusCode) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }

    public static CustomException badRequest(String message) {
        return new CustomException("BAD_REQUEST", message, 400);
    }

    public static CustomException unauthorized(String message) {
        return new CustomException("UNAUTHORIZED", message, 401);
    }

    public static CustomException forbidden(String message) {
        return new CustomException("FORBIDDEN", message, 403);
    }

    public static CustomException notFound(String message) {
        return new CustomException("NOT_FOUND", message, 404);
    }

    public static CustomException conflict(String message) {
        return new CustomException("CONFLICT", message, 409);
    }

    public static CustomException internalServerError(String message) {
        return new CustomException("INTERNAL_SERVER_ERROR", message, 500);
    }
}
