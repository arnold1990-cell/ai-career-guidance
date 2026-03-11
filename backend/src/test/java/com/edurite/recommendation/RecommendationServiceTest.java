package com.edurite.recommendation;

import com.edurite.recommendation.dto.RecommendationResultDto;
import com.edurite.recommendation.service.RecommendationService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentService;
import com.edurite.subscription.repository.SubscriptionRepository;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    StudentService studentService;
    @Mock
    SubscriptionRepository subscriptionRepository;

    private RecommendationService recommendationService;
    private Principal principal;
    private StudentProfile profile;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(studentService, subscriptionRepository);
        principal = () -> "student@example.com";

        profile = new StudentProfile();
        profile.setUserId(UUID.randomUUID());
        profile.setInterests(null);
        profile.setSkills(null);
        profile.setExperience(null);
        profile.setQualificationLevel(null);
        profile.setProfileCompleted(false);
    }

    @Test
    void generateForStudentHandlesNullProfileFieldsSafely() {
        when(studentService.getProfileEntity(principal)).thenReturn(profile);
        when(subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(profile.getUserId())).thenReturn(Optional.empty());

        RecommendationResultDto result = recommendationService.generateForStudent(principal);

        assertThat(result.suggestedCareers()).isNotEmpty();
        assertThat(result.suggestedBursaries()).isNotEmpty();
        assertThat(result.suggestedCoursesOrImprovements()).isNotEmpty();
        assertThat(result.profileImprovementTips()).isNotEmpty();
        assertThat(result.modelVersion()).isEqualTo("rule-engine-v3");
    }
}

