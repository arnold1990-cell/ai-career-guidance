package com.edurite.upload.service; // declares the package path for this Java file

import org.springframework.stereotype.Service; // imports a class so it can be used in this file

// @Service marks a class that contains business logic.
@Service // marks this class as a service containing business logic
/**
 * This class named StorageService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class StorageService { // defines a class type

    /**
     * Note: this method handles the "putObject" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public String putObject(String bucket, String objectName, byte[] bytes) { // declares a method that defines behavior for this class
        return "s3://" + bucket + "/" + objectName;
    } // ends the current code block
} // ends the current code block
