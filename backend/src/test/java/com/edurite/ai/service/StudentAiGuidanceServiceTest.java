package com.edurite.ai.service;

import com.edurite.ai.dto.AiDashboardSummaryResponse;
import com.edurite.ai.dto.CareerAdviceResponse;
import com.edurite.bursary.dto.BursaryResultDto;
import com.edurite.bursary.service.BursaryRecommendationService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentService;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentAiGuidanceServiceTest {

    @Test
    void dashboardSummaryCombinesDashboardCareerAndBursarySignals() {
        StudentService studentService = mock(StudentService.class);
        GeminiService geminiService = mock(GeminiService.class);
        BursaryRecommendationService bursaryRecommendationService = mock(BursaryRecommendationService.class);
        StudentAiGuidanceService service = new StudentAiGuidanceService(studentService, geminiService, bursaryRecommendationService);

        StudentProfile profile = new StudentProfile();
        profile.setQualificationLevel("Degree");
        profile.setInterests("technology");
        profile.setSkills("java");
        profile.setLocation("Gauteng");

        when(studentService.getProfileEntity(any())).thenReturn(profile);
        when(studentService.dashboard(any())).thenReturn(Map.of("savedOpportunities", 3, "skillGaps", List.of("communication")));
        when(geminiService.getCareerAdvice(any())).thenReturn(new CareerAdviceResponse(List.of(
                new CareerAdviceResponse.RecommendedCareer("Software Developer", 88, "Strong fit", List.of("Build projects"))
        )));
        when(bursaryRecommendationService.recommendForStudent(any())).thenReturn(List.of(
                new BursaryResultDto("1", "STEM Bursary", "Provider", "desc", "Degree", "Gauteng", "citizen", null, "https://example.org", "OFFICIAL_PROVIDER", 80, List.of("https://example.org"), true, false, null)
        ));

        AiDashboardSummaryResponse response = service.dashboardSummary((Principal) () -> "student@test");

        assertThat(response.dashboard()).containsEntry("savedOpportunities", 3);
        assertThat(response.recommendedCareers()).extracting(CareerAdviceResponse.RecommendedCareer::name)
                .containsExactly("Software Developer");
        assertThat(response.bursarySuggestions()).hasSize(1);
        assertThat(response.dashboardInsights().get(1)).contains("official provider");
    }
}
