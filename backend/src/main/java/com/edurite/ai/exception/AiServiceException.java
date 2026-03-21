package com.edurite.ai.exception;

import org.springframework.http.HttpStatus;

public class AiServiceException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public AiServiceException(HttpStatus status, String message) {
        this(status, "AI_SERVICE_ERROR", message);
    }

    public AiServiceException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
