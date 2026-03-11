package com.edurite.notification.controller;

import com.edurite.notification.entity.NotificationRecord;
import com.edurite.notification.service.NotificationService;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// @RestController tells Spring this class exposes REST API endpoints.
@RestController
// @RequestMapping defines the base URL path for endpoints in this controller.
@RequestMapping("/api/v1/notifications")
/**
 * This class named NotificationController is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping
    /**
     * Beginner note: this method handles the "mine" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<NotificationRecord> mine(Principal principal) {
        return notificationService.mine(principal);
    }

// @PatchMapping handles HTTP PATCH requests for partial updates.
    @PatchMapping("/{id}/read")
    /**
     * Beginner note: this method handles the "markRead" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public NotificationRecord markRead(Principal principal, @PathVariable String id) {
        return notificationService.markRead(principal, id);
    }
}
