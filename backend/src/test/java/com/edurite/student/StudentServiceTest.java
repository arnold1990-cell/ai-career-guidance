package com.edurite.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.edurite.application.repository.ApplicationRepository;
import com.edurite.notification.repository.NotificationRepository;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.SavedBursaryRepository;
import com.edurite.student.repository.SavedCareerRepository;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.student.service.StudentService;
import com.edurite.subscription.repository.SubscriptionRepository;
import com.edurite.upload.service.StorageService;
import com.edurite.user.entity.User;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private StorageService storageService;
    @Mock
    private SavedCareerRepository savedCareerRepository;
    @Mock
    private SavedBursaryRepository savedBursaryRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;

    private StudentService studentService;
    private Principal principal;
    private User user;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(
                studentProfileRepository,
                currentUserService,
                storageService,
                savedCareerRepository,
                savedBursaryRepository,
                applicationRepository,
                notificationRepository,
                subscriptionRepository
        );
        principal = () -> "student@example.com";
        user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("Demo");
        user.setLastName("Student");
    }

    @Test
    void dashboardReturnsExpectedCountsForStudent() {
        StudentProfile profile = new StudentProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(user.getId());
        profile.setFirstName("Demo");
        profile.setLastName("Student");

        when(currentUserService.requireUser(principal)).thenReturn(user);
        when(studentProfileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));
        when(savedCareerRepository.countByStudentId(profile.getId())).thenReturn(2L);
        when(savedBursaryRepository.countByStudentId(profile.getId())).thenReturn(3L);
        when(applicationRepository.countByStudentId(profile.getId())).thenReturn(1L);
        when(applicationRepository.countByStudentIdAndStatus(profile.getId(), "DRAFT")).thenReturn(1L);
        when(applicationRepository.countByStudentIdAndStatus(profile.getId(), "SUBMITTED")).thenReturn(0L);
        when(applicationRepository.countByStudentIdAndStatus(profile.getId(), "IN_REVIEW")).thenReturn(0L);
        when(applicationRepository.countByStudentIdAndStatus(profile.getId(), "SHORTLISTED")).thenReturn(0L);
        when(notificationRepository.countByUserIdAndReadFalse(user.getId())).thenReturn(4L);
        when(subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(Optional.empty());

        Map<String, Object> result = studentService.dashboard(principal);

        assertThat(result.get("savedCareers")).isEqualTo(2L);
        assertThat(result.get("savedBursaries")).isEqualTo(3L);
        assertThat(result.get("savedOpportunities")).isEqualTo(5L);
        assertThat(result.get("activeApplications")).isEqualTo(1L);
        assertThat(result.get("subscriptionTier")).isEqualTo("BASIC");
    }

    @Test
    void dashboardCreatesDefaultProfileWhenMissing() {
        when(currentUserService.requireUser(principal)).thenReturn(user);
        when(studentProfileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(studentProfileRepository.save(org.mockito.ArgumentMatchers.any(StudentProfile.class))).thenAnswer(invocation -> {
            StudentProfile saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(Optional.empty());

        Map<String, Object> result = studentService.dashboard(principal);

        assertThat(result.get("subscriptionTier")).isEqualTo("BASIC");
        assertThat(result).containsKeys("savedCareers", "savedBursaries", "notifications", "applicationProgress");
    }

    @Test
    void dashboardFallsBackWhenSubscriptionLookupFails() {
        StudentProfile profile = new StudentProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(user.getId());
        profile.setFirstName("Demo");
        profile.setLastName("Student");

        when(currentUserService.requireUser(principal)).thenReturn(user);
        when(studentProfileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));
        when(subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())).thenThrow(new RuntimeException("missing column"));

        Map<String, Object> result = studentService.dashboard(principal);

        assertThat(result.get("subscriptionTier")).isEqualTo("BASIC");
    }

}
