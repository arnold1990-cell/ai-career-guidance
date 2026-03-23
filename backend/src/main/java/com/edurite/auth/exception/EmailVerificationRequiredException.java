package com.edurite.auth.exception;

public class EmailVerificationRequiredException extends RuntimeException {
    public EmailVerificationRequiredException() {
        super("Please verify your email before signing in.");
    }
}
