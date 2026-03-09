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

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/me")
    public SubscriptionRecord current(Principal principal) {
        return subscriptionService.current(principal);
    }

    @PostMapping("/purchase")
    public Map<String, Object> purchase(Principal principal, @RequestBody Map<String, String> payload) {
        return subscriptionService.purchase(principal, payload.getOrDefault("plan", "BASIC"));
    }
}
