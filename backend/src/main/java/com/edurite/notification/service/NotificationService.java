package com.edurite.notification.service; // declares the package path for this Java file

import com.edurite.notification.entity.NotificationRecord; // imports a class so it can be used in this file
import com.edurite.notification.repository.NotificationRepository; // imports a class so it can be used in this file
import com.edurite.student.repository.StudentProfileRepository; // imports a class so it can be used in this file
import com.edurite.security.service.CurrentUserService; // imports a class so it can be used in this file
import java.security.Principal; // imports a class so it can be used in this file
import java.time.OffsetDateTime; // imports a class so it can be used in this file
import java.util.List; // imports a class so it can be used in this file
import org.springframework.stereotype.Service; // imports a class so it can be used in this file

// @Service marks a class that contains business logic.
@Service // marks this class as a service containing business logic
/**
 * This class named NotificationService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class NotificationService { // defines a class type

    private final NotificationRepository notificationRepository; // reads or writes data through the database layer
    private final CurrentUserService currentUserService; // executes this statement as part of the application logic
    private final StudentProfileRepository studentProfileRepository; // reads or writes data through the database layer

    public NotificationService(NotificationRepository notificationRepository, CurrentUserService currentUserService, StudentProfileRepository studentProfileRepository) { // reads or writes data through the database layer
        this.notificationRepository = notificationRepository; // reads or writes data through the database layer
        this.currentUserService = currentUserService; // executes this statement as part of the application logic
        this.studentProfileRepository = studentProfileRepository; // reads or writes data through the database layer
    } // ends the current code block

    /**
     * Note: this method handles the "mine" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<NotificationRecord> mine(Principal principal) { // declares a method that defines behavior for this class
        var user = currentUserService.requireUser(principal); // executes this statement as part of the application logic
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "markRead" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public NotificationRecord markRead(Principal principal, String id) { // declares a method that defines behavior for this class
        var user = currentUserService.requireUser(principal); // executes this statement as part of the application logic
        NotificationRecord n = notificationRepository.findById(java.util.UUID.fromString(id)).orElseThrow(); // reads or writes data through the database layer
        if (!n.getUserId().equals(user.getId())) throw new IllegalArgumentException("Forbidden"); // checks a condition and runs this block only when true
        n.setRead(true); // executes this statement as part of the application logic
        return notificationRepository.save(n); // returns a value from this method to the caller
    } // ends the current code block

    public void sendEmail(String to, String template) {} // declares a method that defines behavior for this class
    public void sendSms(String to, String template) {} // declares a method that defines behavior for this class

    /**
     * Note: this method handles the "createInApp" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public NotificationRecord createInApp(java.util.UUID userId, String eventType, String title, String message) { // declares a method that defines behavior for this class
        boolean inAppEnabled = studentProfileRepository.findByUserId(userId) // reads or writes data through the database layer
                .map(profile -> profile.isInAppNotificationsEnabled()) // supports the surrounding application logic
                .orElse(true); // executes this statement as part of the application logic
        if (!inAppEnabled) { // checks a condition and runs this block only when true
            return null; // returns a value from this method to the caller
        } // ends the current code block
        NotificationRecord record = new NotificationRecord(); // creates a new object instance and stores it in a variable
        record.setUserId(userId); // executes this statement as part of the application logic
        record.setEventType(eventType); // executes this statement as part of the application logic
        record.setChannel("IN_APP"); // executes this statement as part of the application logic
        record.setTitle(title); // executes this statement as part of the application logic
        record.setMessage(message); // executes this statement as part of the application logic
        record.setSentAt(OffsetDateTime.now()); // executes this statement as part of the application logic
        return notificationRepository.save(record); // returns a value from this method to the caller
    } // ends the current code block
} // ends the current code block
