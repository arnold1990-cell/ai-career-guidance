package com.edurite;

import org.springframework.boot.SpringApplication;
import com.edurite.ai.config.GeminiProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(GeminiProperties.class)
/**
 * This class named EduRiteApplication is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class EduRiteApplication {

    /**
     * this method handles the "main" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public static void main(String[] args) {
        // TODO: bootstrap shared infrastructure and module wiring for the modular monolith.
        SpringApplication.run(EduRiteApplication.class, args);
    }
}
