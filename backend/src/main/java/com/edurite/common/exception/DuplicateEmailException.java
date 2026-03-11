package com.edurite.common.exception; // declares the package path for this Java file

/**
 * This class named DuplicateEmailException is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class DuplicateEmailException extends RuntimeException { // defines a class type
    public DuplicateEmailException(String email) { // declares a method that defines behavior for this class
        super("An account with email '%s' already exists".formatted(email)); // executes this statement as part of the application logic
    } // ends the current code block
} // ends the current code block
