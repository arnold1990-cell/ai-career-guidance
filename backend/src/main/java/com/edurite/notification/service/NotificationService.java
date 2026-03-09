package com.edurite.notification.service;

import com.edurite.notification.entity.NotificationRecord;
import com.edurite.notification.repository.NotificationRepository;
import com.edurite.security.service.CurrentUserService;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;

    public NotificationService(NotificationRepository notificationRepository, CurrentUserService currentUserService) {
        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
    }

    public List<NotificationRecord> mine(Principal principal) {
        var user = currentUserService.requireUser(principal);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public NotificationRecord markRead(Principal principal, String id) {
        var user = currentUserService.requireUser(principal);
        NotificationRecord n = notificationRepository.findById(java.util.UUID.fromString(id)).orElseThrow();
        if (!n.getUserId().equals(user.getId())) throw new IllegalArgumentException("Forbidden");
        n.setRead(true);
        return notificationRepository.save(n);
    }

    public void sendEmail(String to, String template) {}
    public void sendSms(String to, String template) {}

    public NotificationRecord createInApp(java.util.UUID userId, String eventType, String title, String message) {
        NotificationRecord record = new NotificationRecord();
        record.setUserId(userId);
        record.setEventType(eventType);
        record.setChannel("IN_APP");
        record.setTitle(title);
        record.setMessage(message);
        record.setSentAt(OffsetDateTime.now());
        return notificationRepository.save(record);
    }
}
