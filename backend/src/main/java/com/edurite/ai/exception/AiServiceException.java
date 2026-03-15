package com.edurite.ai.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AiServiceException extends RuntimeException {

    private final HttpStatus status;

    public AiServiceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

}
