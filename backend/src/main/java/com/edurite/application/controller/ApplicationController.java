package com.edurite.application.controller; // declares the package path for this Java file

import com.edurite.application.entity.ApplicationRecord; // imports a class so it can be used in this file
import com.edurite.application.service.ApplicationService; // imports a class so it can be used in this file
import java.security.Principal; // imports a class so it can be used in this file
import java.util.List; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.GetMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.PathVariable; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.PostMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RequestMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RestController; // imports a class so it can be used in this file

// @RestController tells Spring this class exposes REST API endpoints.
@RestController // marks this class as a REST controller that handles HTTP requests
// @RequestMapping defines the base URL path for endpoints in this controller.
@RequestMapping("/api/v1") // sets the base URL path for endpoints in this controller
/**
 * This class named ApplicationController is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class ApplicationController { // defines a class type

    private final ApplicationService applicationService; // executes this statement as part of the application logic

    public ApplicationController(ApplicationService applicationService) { // declares a method that defines behavior for this class
        this.applicationService = applicationService; // executes this statement as part of the application logic
    } // ends the current code block

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/bursaries/{id}/applications") // maps this method to handle HTTP POST requests
    /**
     * Note: this method handles the "apply" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ApplicationRecord apply(@PathVariable UUID id, Principal principal) { // declares a method that defines behavior for this class
        return applicationService.submit(id, principal); // returns a value from this method to the caller
    } // ends the current code block

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/applications/me") // maps this method to handle HTTP GET requests
    /**
     * Note: this method handles the "myApplications" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<ApplicationRecord> myApplications(Principal principal) { // declares a method that defines behavior for this class
        return applicationService.listMine(principal); // returns a value from this method to the caller
    } // ends the current code block
} // ends the current code block
