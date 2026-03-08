package com.edurite.common.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email) {
        super("An account with email '%s' already exists".formatted(email));
    }
}
