package com.edurite.notification.service;

import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    public void sendEmail(String to, String template) {
        // Placeholder for SMTP provider integration.
    }

    public void sendSms(String to, String template) {
        // Placeholder for SMS provider integration.
    }

    public void sendPush(String device, String template) {
        // Placeholder for push provider integration.
    }
}
