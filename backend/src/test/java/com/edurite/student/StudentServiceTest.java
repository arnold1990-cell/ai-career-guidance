package com.edurite.student; // declares the package path for this Java file

import com.edurite.application.repository.ApplicationRepository; // imports a class so it can be used in this file
import com.edurite.notification.repository.NotificationRepository; // imports a class so it can be used in this file
import com.edurite.security.service.CurrentUserService; // imports a class so it can be used in this file
import com.edurite.student.dto.StudentSettingsDto; // imports a class so it can be used in this file
import com.edurite.student.entity.StudentProfile; // imports a class so it can be used in this file
import com.edurite.student.repository.SavedBursaryRepository; // imports a class so it can be used in this file
import com.edurite.student.repository.SavedCareerRepository; // imports a class so it can be used in this file
import com.edurite.student.repository.StudentProfileRepository; // imports a class so it can be used in this file
import com.edurite.student.service.StudentService; // imports a class so it can be used in this file
import com.edurite.subscription.entity.SubscriptionRecord; // imports a class so it can be used in this file
import com.edurite.subscription.repository.SubscriptionRepository; // imports a class so it can be used in this file
import com.edurite.upload.service.StorageService; // imports a class so it can be used in this file
import com.edurite.user.entity.User; // imports a class so it can be used in this file
import org.junit.jupiter.api.BeforeEach; // imports a class so it can be used in this file
import org.junit.jupiter.api.Test; // imports a class so it can be used in this file
import org.junit.jupiter.api.extension.ExtendWith; // imports a class so it can be used in this file
import org.mockito.Mock; // imports a class so it can be used in this file
import org.mockito.junit.jupiter.MockitoExtension; // imports a class so it can be used in this file

import java.security.Principal; // imports a class so it can be used in this file
import java.util.Map; // imports a class so it can be used in this file
import java.util.Optional; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file

import static org.assertj.core.api.Assertions.assertThat; // imports a class so it can be used in this file
import static org.mockito.ArgumentMatchers.any; // imports a class so it can be used in this file
import static org.mockito.Mockito.when; // imports a class so it can be used in this file

@ExtendWith(MockitoExtension.class) // adds metadata that Spring or Java uses at runtime
class StudentServiceTest { // defines a class type

    @Mock // adds metadata that Spring or Java uses at runtime
    StudentProfileRepository profileRepository; // reads or writes data through the database layer
    @Mock // adds metadata that Spring or Java uses at runtime
    CurrentUserService currentUserService; // executes this statement as part of the application logic
    @Mock // adds metadata that Spring or Java uses at runtime
    StorageService storageService; // executes this statement as part of the application logic
    @Mock // adds metadata that Spring or Java uses at runtime
    SavedCareerRepository savedCareerRepository; // reads or writes data through the database layer
    @Mock // adds metadata that Spring or Java uses at runtime
    SavedBursaryRepository savedBursaryRepository; // reads or writes data through the database layer
    @Mock // adds metadata that Spring or Java uses at runtime
    ApplicationRepository applicationRepository; // reads or writes data through the database layer
    @Mock // adds metadata that Spring or Java uses at runtime
    NotificationRepository notificationRepository; // reads or writes data through the database layer
    @Mock // adds metadata that Spring or Java uses at runtime
    SubscriptionRepository subscriptionRepository; // reads or writes data through the database layer

    private StudentService studentService; // executes this statement as part of the application logic
    private User user; // executes this statement as part of the application logic
    private Principal principal; // executes this statement as part of the application logic

    @BeforeEach // adds metadata that Spring or Java uses at runtime
    void setUp() { // supports the surrounding application logic
        studentService = new StudentService( // creates a new object instance and stores it in a variable
                profileRepository, // reads or writes data through the database layer
                currentUserService, // supports the surrounding application logic
                storageService, // supports the surrounding application logic
                savedCareerRepository, // reads or writes data through the database layer
                savedBursaryRepository, // reads or writes data through the database layer
                applicationRepository, // reads or writes data through the database layer
                notificationRepository, // reads or writes data through the database layer
                subscriptionRepository // reads or writes data through the database layer
        ); // executes this statement as part of the application logic

        user = new User(); // creates a new object instance and stores it in a variable
        user.setId(UUID.randomUUID()); // executes this statement as part of the application logic
        user.setEmail("student@example.com"); // executes this statement as part of the application logic
        user.setFirstName("Test"); // executes this statement as part of the application logic
        user.setLastName("Student"); // executes this statement as part of the application logic

        principal = () -> user.getEmail(); // executes this statement as part of the application logic
        when(currentUserService.requireUser(principal)).thenReturn(user); // executes this statement as part of the application logic
    } // ends the current code block

    @Test // adds metadata that Spring or Java uses at runtime
    void dashboardFallsBackToBasicWhenSubscriptionPlanCodeIsNull() { // supports the surrounding application logic
        StudentProfile profile = new StudentProfile(); // creates a new object instance and stores it in a variable
        profile.setId(UUID.randomUUID()); // executes this statement as part of the application logic
        profile.setUserId(user.getId()); // executes this statement as part of the application logic
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile)); // reads or writes data through the database layer

        SubscriptionRecord subscription = new SubscriptionRecord(); // creates a new object instance and stores it in a variable
        subscription.setPlanCode(null); // executes this statement as part of the application logic
        when(subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(Optional.of(subscription)); // reads or writes data through the database layer
        when(savedCareerRepository.countByStudentId(profile.getId())).thenReturn(0L); // reads or writes data through the database layer
        when(savedBursaryRepository.countByStudentId(profile.getId())).thenReturn(0L); // reads or writes data through the database layer
        when(applicationRepository.countByStudentId(profile.getId())).thenReturn(0L); // reads or writes data through the database layer
        when(applicationRepository.countByStudentIdAndStatus(profile.getId(), "DRAFT")).thenReturn(0L); // reads or writes data through the database layer
        when(applicationRepository.countByStudentIdAndStatus(profile.getId(), "SUBMITTED")).thenReturn(0L); // reads or writes data through the database layer
        when(applicationRepository.countByStudentIdAndStatus(profile.getId(), "IN_REVIEW")).thenReturn(0L); // reads or writes data through the database layer
        when(applicationRepository.countByStudentIdAndStatus(profile.getId(), "SHORTLISTED")).thenReturn(0L); // reads or writes data through the database layer
        when(notificationRepository.countByUserIdAndReadFalse(user.getId())).thenReturn(0L); // reads or writes data through the database layer

        Map<String, Object> result = studentService.dashboard(principal); // executes this statement as part of the application logic

        assertThat(result).containsEntry("subscriptionTier", "BASIC"); // executes this statement as part of the application logic
    } // ends the current code block

    @Test // adds metadata that Spring or Java uses at runtime
    void getSettingsCreatesDefaultProfileWhenMissing() { // supports the surrounding application logic
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.empty()); // reads or writes data through the database layer
        when(profileRepository.save(any(StudentProfile.class))).thenAnswer(invocation -> { // defines a class type
            StudentProfile saved = invocation.getArgument(0); // executes this statement as part of the application logic
            saved.setId(UUID.randomUUID()); // executes this statement as part of the application logic
            return saved; // returns a value from this method to the caller
        }); // executes this statement as part of the application logic

        StudentSettingsDto settings = studentService.getSettings(principal); // executes this statement as part of the application logic

        assertThat(settings.inAppNotificationsEnabled()).isTrue(); // executes this statement as part of the application logic
        assertThat(settings.emailNotificationsEnabled()).isFalse(); // executes this statement as part of the application logic
        assertThat(settings.smsNotificationsEnabled()).isFalse(); // executes this statement as part of the application logic
    } // ends the current code block
} // ends the current code block
