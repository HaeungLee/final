package com.agentica.user.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class ErrorResponse {
    private String errorCode;
    private String message;
    private Map<String, String> details;
}
