package com.edurite.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
/**
 * Registers AI-related configuration properties as Spring beans.
 */
public class AiConfig {
}
