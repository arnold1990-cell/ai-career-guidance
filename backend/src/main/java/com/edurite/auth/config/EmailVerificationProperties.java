package com.edurite.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edurite.auth.verification")
public record EmailVerificationProperties(String frontendUrlBase, long tokenValidityHours) {
}
