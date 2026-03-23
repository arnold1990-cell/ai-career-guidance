package com.edurite.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edurite.mail")
public record MailSenderProperties(String senderEmail) {
}
