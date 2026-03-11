package com.edurite.subscription.controller;

import com.edurite.subscription.entity.SubscriptionRecord;
import com.edurite.subscription.service.SubscriptionService;
import java.security.Principal;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// @RestController tells Spring this class exposes REST API endpoints.
@RestController
// @RequestMapping defines the base URL path for endpoints in this controller.
@RequestMapping("/api/v1/subscriptions")
/**
 * This class named SubscriptionController is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/me")
    /**
     * Beginner note: this method handles the "current" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public SubscriptionRecord current(Principal principal) {
        return subscriptionService.current(principal);
    }

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/purchase")
    /**
     * Beginner note: this method handles the "purchase" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public Map<String, Object> purchase(Principal principal, @RequestBody Map<String, String> payload) {
        return subscriptionService.purchase(principal, payload.getOrDefault("plan", "BASIC"));
    }
}
