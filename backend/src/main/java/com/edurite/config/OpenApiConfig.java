package com.edurite.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI eduriteOpenApi() {
        return new OpenAPI().info(new Info().title("EduRite API").version("v1").description("EduRite MVP API"));
    }
}
