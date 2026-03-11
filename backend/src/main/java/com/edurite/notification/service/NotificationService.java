package com.edurite.notification.service;

import com.edurite.notification.entity.NotificationRecord;
import com.edurite.notification.repository.NotificationRepository;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.security.service.CurrentUserService;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

// @Service marks a class that contains business logic.
@Service
/**
 * This class named NotificationService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;
    private final StudentProfileRepository studentProfileRepository;

    public NotificationService(NotificationRepository notificationRepository, CurrentUserService currentUserService, StudentProfileRepository studentProfileRepository) {
        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
        this.studentProfileRepository = studentProfileRepository;
    }

    /**
     * Beginner note: this method handles the "mine" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<NotificationRecord> mine(Principal principal) {
        var user = currentUserService.requireUser(principal);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    /**
     * Beginner note: this method handles the "markRead" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public NotificationRecord markRead(Principal principal, String id) {
        var user = currentUserService.requireUser(principal);
        NotificationRecord n = notificationRepository.findById(java.util.UUID.fromString(id)).orElseThrow();
        if (!n.getUserId().equals(user.getId())) throw new IllegalArgumentException("Forbidden");
        n.setRead(true);
        return notificationRepository.save(n);
    }

    public void sendEmail(String to, String template) {}
    public void sendSms(String to, String template) {}

    /**
     * Beginner note: this method handles the "createInApp" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public NotificationRecord createInApp(java.util.UUID userId, String eventType, String title, String message) {
        boolean inAppEnabled = studentProfileRepository.findByUserId(userId)
                .map(profile -> profile.isInAppNotificationsEnabled())
                .orElse(true);
        if (!inAppEnabled) {
            return null;
        }
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
