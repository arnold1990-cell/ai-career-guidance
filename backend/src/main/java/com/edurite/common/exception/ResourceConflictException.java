package com.edurite.common.exception; // declares the package path for this Java file

/**
 * This class named ResourceConflictException is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class ResourceConflictException extends RuntimeException { // defines a class type
    public ResourceConflictException(String message) { // declares a method that defines behavior for this class
        super(message); // executes this statement as part of the application logic
    } // ends the current code block
} // ends the current code block
