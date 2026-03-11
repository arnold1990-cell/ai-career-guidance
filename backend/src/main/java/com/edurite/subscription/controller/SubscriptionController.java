package com.edurite.subscription.controller; // declares the package path for this Java file

import com.edurite.subscription.entity.SubscriptionRecord; // imports a class so it can be used in this file
import com.edurite.subscription.service.SubscriptionService; // imports a class so it can be used in this file
import java.security.Principal; // imports a class so it can be used in this file
import java.util.Map; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.GetMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.PostMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RequestBody; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RequestMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RestController; // imports a class so it can be used in this file

// @RestController tells Spring this class exposes REST API endpoints.
@RestController // marks this class as a REST controller that handles HTTP requests
// @RequestMapping defines the base URL path for endpoints in this controller.
@RequestMapping("/api/v1/subscriptions") // sets the base URL path for endpoints in this controller
/**
 * This class named SubscriptionController is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class SubscriptionController { // defines a class type
    private final SubscriptionService subscriptionService; // executes this statement as part of the application logic

    public SubscriptionController(SubscriptionService subscriptionService) { // declares a method that defines behavior for this class
        this.subscriptionService = subscriptionService; // executes this statement as part of the application logic
    } // ends the current code block

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/me") // maps this method to handle HTTP GET requests
    /**
     * Note: this method handles the "current" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public SubscriptionRecord current(Principal principal) { // declares a method that defines behavior for this class
        return subscriptionService.current(principal); // returns a value from this method to the caller
    } // ends the current code block

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/purchase") // maps this method to handle HTTP POST requests
    /**
     * Note: this method handles the "purchase" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public Map<String, Object> purchase(Principal principal, @RequestBody Map<String, String> payload) { // declares a method that defines behavior for this class
        return subscriptionService.purchase(principal, payload.getOrDefault("plan", "BASIC")); // returns a value from this method to the caller
    } // ends the current code block
} // ends the current code block
