package com.edurite.student;

import com.edurite.application.repository.ApplicationRepository;
import com.edurite.bursary.repository.BursaryRepository;
import com.edurite.notification.repository.NotificationRepository;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.dto.StudentSettingsDto;
import com.edurite.student.entity.SavedCareer;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.SavedBursaryRepository;
import com.edurite.student.repository.SavedCareerRepository;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.student.service.StudentService;
import com.edurite.subscription.entity.SubscriptionRecord;
import com.edurite.subscription.repository.SubscriptionRepository;
import com.edurite.upload.service.StorageService;
import com.edurite.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    StudentProfileRepository profileRepository;
    @Mock
    CurrentUserService currentUserService;
    @Mock
    StorageService storageService;
    @Mock
    SavedCareerRepository savedCareerRepository;
    @Mock
    SavedBursaryRepository savedBursaryRepository;
    @Mock
    ApplicationRepository applicationRepository;
    @Mock
    NotificationRepository notificationRepository;
    @Mock
    SubscriptionRepository subscriptionRepository;
    @Mock
    BursaryRepository bursaryRepository;

    private StudentService studentService;
    private User user;
    private Principal principal;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(
                profileRepository,
                currentUserService,
                storageService,
                savedCareerRepository,
                savedBursaryRepository,
                applicationRepository,
                notificationRepository,
                subscriptionRepository,
                bursaryRepository
        );

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("student@example.com");
        user.setFirstName("Test");
        user.setLastName("Student");

        principal = () -> user.getEmail();
        when(currentUserService.requireUser(principal)).thenReturn(user);
    }

    @Test
    void dashboardFallsBackToBasicWhenSubscriptionPlanCodeIsNull() {
        StudentProfile profile = new StudentProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(user.getId());
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));

        SubscriptionRecord subscription = new SubscriptionRecord();
        subscription.setPlanCode(null);
        when(subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(Optional.of(subscription));
        when(savedCareerRepository.countByStudentId(profile.getId())).thenReturn(0L);
        when(savedBursaryRepository.countByStudentId(profile.getId())).thenReturn(0L);
        when(applicationRepository.countByStudentId(profile.getId())).thenReturn(0L);
        when(applicationRepository.countByStudentIdAndStatus(profile.getId(), "DRAFT")).thenReturn(0L);
        when(applicationRepository.countByStudentIdAndStatus(profile.getId(), "SUBMITTED")).thenReturn(0L);
        when(applicationRepository.countByStudentIdAndStatus(profile.getId(), "IN_REVIEW")).thenReturn(0L);
        when(applicationRepository.countByStudentIdAndStatus(profile.getId(), "SHORTLISTED")).thenReturn(0L);
        when(notificationRepository.countByUserIdAndReadFalse(user.getId())).thenReturn(0L);

        Map<String, Object> result = studentService.dashboard(principal);

        assertThat(result).containsEntry("subscriptionTier", "BASIC");
    }


    @Test
    void savedOpportunityKeysIncludeCareersAndInternships() {
        StudentProfile profile = new StudentProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(user.getId());
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));

        SavedCareer savedCareer = new SavedCareer();
        savedCareer.setCareerId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        SavedCareer savedInternship = new SavedCareer();
        savedInternship.setOpportunityType("INTERNSHIP");
        savedInternship.setExternalKey("internship-data-analyst-intern");

        when(savedCareerRepository.findByStudentId(profile.getId())).thenReturn(java.util.List.of(savedCareer, savedInternship));

        assertThat(studentService.savedOpportunityKeys(principal))
                .containsExactly(
                        "CAREER:11111111-1111-1111-1111-111111111111",
                        "INTERNSHIP:internship-data-analyst-intern"
                );
    }

    @Test
    void getSettingsCreatesDefaultProfileWhenMissing() {
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(profileRepository.save(any(StudentProfile.class))).thenAnswer(invocation -> {
            StudentProfile saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        StudentSettingsDto settings = studentService.getSettings(principal);

        assertThat(settings.inAppNotificationsEnabled()).isTrue();
        assertThat(settings.emailNotificationsEnabled()).isFalse();
        assertThat(settings.smsNotificationsEnabled()).isFalse();
    }
}
