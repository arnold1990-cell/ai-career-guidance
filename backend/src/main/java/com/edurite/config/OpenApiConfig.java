package com.edurite.config; // declares the package path for this Java file

import io.swagger.v3.oas.models.OpenAPI; // imports a class so it can be used in this file
import io.swagger.v3.oas.models.info.Info; // imports a class so it can be used in this file
import org.springframework.context.annotation.Bean; // imports a class so it can be used in this file
import org.springframework.context.annotation.Configuration; // imports a class so it can be used in this file

// @Configuration marks a class that defines Spring beans and setup.
@Configuration // marks this class as a Spring configuration class
/**
 * This class named OpenApiConfig is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class OpenApiConfig { // defines a class type

// @Bean tells Spring to register this method return value in the dependency injection container.
    @Bean // registers this method return value as a Spring bean
    OpenAPI eduriteOpenApi() { // supports the surrounding application logic
        return new OpenAPI().info(new Info().title("EduRite API").version("v1").description("EduRite MVP API")); // returns a value from this method to the caller
    } // ends the current code block
} // ends the current code block
