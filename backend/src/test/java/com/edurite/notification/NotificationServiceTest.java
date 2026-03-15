package com.edurite.notification;

import com.edurite.notification.events.CareerInsightUpdateEvent;
import com.edurite.notification.repository.NotificationRepository;
import com.edurite.notification.service.NotificationService;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    @Test
    void eventHandlerNotifiesStudentsBasedOnPreferences() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        StudentProfileRepository studentProfileRepository = mock(StudentProfileRepository.class);
        NotificationService service = new NotificationService(notificationRepository, currentUserService, studentProfileRepository);

        StudentProfile profile = new StudentProfile();
        profile.setUserId(UUID.randomUUID());
        profile.setInAppNotificationsEnabled(true);
        profile.setEmailNotificationsEnabled(false);
        profile.setSmsNotificationsEnabled(false);
        when(studentProfileRepository.findAll()).thenReturn(List.of(profile));
        when(studentProfileRepository.findByUserId(profile.getUserId())).thenReturn(java.util.Optional.of(profile));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.onCareerInsightUpdate(new CareerInsightUpdateEvent("Insight", "Message"));

        verify(notificationRepository, atLeastOnce()).save(any());
    }
}
