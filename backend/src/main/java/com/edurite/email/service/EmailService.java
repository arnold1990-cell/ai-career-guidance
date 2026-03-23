package com.edurite.email.service;

public interface EmailService {
    void sendEmailVerification(String toEmail, String recipientName, String verificationUrl, long expiresInHours);
}
