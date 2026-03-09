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

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationRecord> mine(Principal principal) {
        return notificationService.mine(principal);
    }

    @PatchMapping("/{id}/read")
    public NotificationRecord markRead(Principal principal, @PathVariable String id) {
        return notificationService.markRead(principal, id);
    }
}
