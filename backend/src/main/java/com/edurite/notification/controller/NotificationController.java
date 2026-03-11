package com.edurite.notification.controller; // declares the package path for this Java file

import com.edurite.notification.entity.NotificationRecord; // imports a class so it can be used in this file
import com.edurite.notification.service.NotificationService; // imports a class so it can be used in this file
import java.security.Principal; // imports a class so it can be used in this file
import java.util.List; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.GetMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.PatchMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.PathVariable; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RequestMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RestController; // imports a class so it can be used in this file

// @RestController tells Spring this class exposes REST API endpoints.
@RestController // marks this class as a REST controller that handles HTTP requests
// @RequestMapping defines the base URL path for endpoints in this controller.
@RequestMapping("/api/v1/notifications") // sets the base URL path for endpoints in this controller
/**
 * This class named NotificationController is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class NotificationController { // defines a class type

    private final NotificationService notificationService; // executes this statement as part of the application logic

    public NotificationController(NotificationService notificationService) { // declares a method that defines behavior for this class
        this.notificationService = notificationService; // executes this statement as part of the application logic
    } // ends the current code block

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping // maps this method to handle HTTP GET requests
    /**
     * Note: this method handles the "mine" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<NotificationRecord> mine(Principal principal) { // declares a method that defines behavior for this class
        return notificationService.mine(principal); // returns a value from this method to the caller
    } // ends the current code block

// @PatchMapping handles HTTP PATCH requests for partial updates.
    @PatchMapping("/{id}/read") // maps this method to handle HTTP PATCH requests
    /**
     * Note: this method handles the "markRead" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public NotificationRecord markRead(Principal principal, @PathVariable String id) { // declares a method that defines behavior for this class
        return notificationService.markRead(principal, id); // returns a value from this method to the caller
    } // ends the current code block
} // ends the current code block
