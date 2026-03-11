package com.edurite.recommendation; // declares the package path for this Java file

import com.edurite.recommendation.dto.RecommendationResultDto; // imports a class so it can be used in this file
import com.edurite.recommendation.service.RecommendationService; // imports a class so it can be used in this file
import com.edurite.student.entity.StudentProfile; // imports a class so it can be used in this file
import com.edurite.student.service.StudentService; // imports a class so it can be used in this file
import com.edurite.subscription.repository.SubscriptionRepository; // imports a class so it can be used in this file
import java.security.Principal; // imports a class so it can be used in this file
import java.util.Optional; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import org.junit.jupiter.api.BeforeEach; // imports a class so it can be used in this file
import org.junit.jupiter.api.Test; // imports a class so it can be used in this file
import org.junit.jupiter.api.extension.ExtendWith; // imports a class so it can be used in this file
import org.mockito.Mock; // imports a class so it can be used in this file
import org.mockito.junit.jupiter.MockitoExtension; // imports a class so it can be used in this file

import static org.assertj.core.api.Assertions.assertThat; // imports a class so it can be used in this file
import static org.mockito.Mockito.when; // imports a class so it can be used in this file

@ExtendWith(MockitoExtension.class) // adds metadata that Spring or Java uses at runtime
class RecommendationServiceTest { // defines a class type

    @Mock // adds metadata that Spring or Java uses at runtime
    StudentService studentService; // executes this statement as part of the application logic
    @Mock // adds metadata that Spring or Java uses at runtime
    SubscriptionRepository subscriptionRepository; // reads or writes data through the database layer

    private RecommendationService recommendationService; // executes this statement as part of the application logic
    private Principal principal; // executes this statement as part of the application logic
    private StudentProfile profile; // executes this statement as part of the application logic

    @BeforeEach // adds metadata that Spring or Java uses at runtime
    void setUp() { // supports the surrounding application logic
        recommendationService = new RecommendationService(studentService, subscriptionRepository); // creates a new object instance and stores it in a variable
        principal = () -> "student@example.com"; // executes this statement as part of the application logic

        profile = new StudentProfile(); // creates a new object instance and stores it in a variable
        profile.setUserId(UUID.randomUUID()); // executes this statement as part of the application logic
        profile.setInterests(null); // executes this statement as part of the application logic
        profile.setSkills(null); // executes this statement as part of the application logic
        profile.setExperience(null); // executes this statement as part of the application logic
        profile.setQualificationLevel(null); // executes this statement as part of the application logic
        profile.setProfileCompleted(false); // executes this statement as part of the application logic
    } // ends the current code block

    @Test // adds metadata that Spring or Java uses at runtime
    void generateForStudentHandlesNullProfileFieldsSafely() { // supports the surrounding application logic
        when(studentService.getProfileEntity(principal)).thenReturn(profile); // executes this statement as part of the application logic
        when(subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(profile.getUserId())).thenReturn(Optional.empty()); // reads or writes data through the database layer

        RecommendationResultDto result = recommendationService.generateForStudent(principal); // executes this statement as part of the application logic

        assertThat(result.suggestedCareers()).isNotEmpty(); // executes this statement as part of the application logic
        assertThat(result.suggestedBursaries()).isNotEmpty(); // executes this statement as part of the application logic
        assertThat(result.suggestedCoursesOrImprovements()).isNotEmpty(); // executes this statement as part of the application logic
        assertThat(result.profileImprovementTips()).isNotEmpty(); // executes this statement as part of the application logic
        assertThat(result.modelVersion()).isEqualTo("rule-engine-v3"); // executes this statement as part of the application logic
    } // ends the current code block
} // ends the current code block

