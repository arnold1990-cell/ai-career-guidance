package com.edurite; // declares the package path for this Java file

import org.springframework.boot.SpringApplication; // imports a class so it can be used in this file
import org.springframework.boot.autoconfigure.SpringBootApplication; // imports a class so it can be used in this file

@SpringBootApplication // adds metadata that Spring or Java uses at runtime
/**
 * This class named EduRiteApplication is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class EduRiteApplication { // defines a class type

    /**
     * Note: this method handles the "main" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public static void main(String[] args) { // declares a method that defines behavior for this class
        // TODO: bootstrap shared infrastructure and module wiring for the modular monolith.
        SpringApplication.run(EduRiteApplication.class, args); // defines a class type
    } // ends the current code block
} // ends the current code block
