package com.edurite.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.edurite.recommendation.dto.RecommendationResultDto;
import com.edurite.recommendation.service.RecommendationService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentService;
import com.edurite.subscription.repository.SubscriptionRepository;
import com.edurite.subscription.entity.SubscriptionRecord;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private StudentService studentService;
    @Mock
    private SubscriptionRepository subscriptionRepository;

    private RecommendationService recommendationService;
    private Principal principal;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(studentService, subscriptionRepository);
        principal = () -> "student@example.com";
    }

    @Test
    void generateForStudentReturnsRecommendationsForAuthenticatedStudent() {
        StudentProfile profile = new StudentProfile();
        profile.setUserId(UUID.randomUUID());
        profile.setSkills("java,programming");
        profile.setInterests("technology");
        profile.setProfileCompleted(true);

        SubscriptionRecord subscriptionRecord = new SubscriptionRecord();
        subscriptionRecord.setPlanCode("PLAN_PREMIUM");
        subscriptionRecord.setStatus("ACTIVE");

        when(studentService.getProfileEntity(principal)).thenReturn(profile);
        when(subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(profile.getUserId())).thenReturn(Optional.of(subscriptionRecord));

        RecommendationResultDto result = recommendationService.generateForStudent(principal);

        assertThat(result.suggestedCareers()).isNotEmpty();
        assertThat(result.suggestedBursaries()).isNotEmpty();
        assertThat(result.improvements()).extracting("id").contains("course-data-structures");
    }

    @Test
    void generateForStudentReturnsDefaultsWhenProfileIsSparse() {
        StudentProfile profile = new StudentProfile();
        profile.setUserId(UUID.randomUUID());
        profile.setProfileCompleted(false);

        when(studentService.getProfileEntity(principal)).thenReturn(profile);
        when(subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(profile.getUserId())).thenReturn(Optional.empty());

        RecommendationResultDto result = recommendationService.generateForStudent(principal);

        assertThat(result.suggestedCareers()).isNotEmpty();
        assertThat(result.suggestedBursaries()).isNotEmpty();
        assertThat(result.profileTips()).isNotEmpty();
    }

    @Test
    void generateForStudentHandlesSubscriptionLookupFailure() {
        StudentProfile profile = new StudentProfile();
        profile.setUserId(UUID.randomUUID());
        profile.setInterests("technology");

        when(studentService.getProfileEntity(principal)).thenReturn(profile);
        when(subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(profile.getUserId())).thenThrow(new RuntimeException("broken query"));

        RecommendationResultDto result = recommendationService.generateForStudent(principal);

        assertThat(result.suggestedCareers()).isNotEmpty();
        assertThat(result.improvements()).extracting("id").contains("upgrade-premium");
    }

}
