package com.edurite.ai.service;

import com.edurite.ai.dto.AiDashboardSummaryResponse;
import com.edurite.ai.dto.CareerAdviceRequest;
import com.edurite.ai.dto.CareerAdviceResponse;
import com.edurite.bursary.dto.BursaryResultDto;
import com.edurite.bursary.service.BursaryRecommendationService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentService;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class StudentAiGuidanceService {

    private final StudentService studentService;
    private final GeminiService geminiService;
    private final BursaryRecommendationService bursaryRecommendationService;

    public StudentAiGuidanceService(StudentService studentService,
                                    GeminiService geminiService,
                                    BursaryRecommendationService bursaryRecommendationService) {
        this.studentService = studentService;
        this.geminiService = geminiService;
        this.bursaryRecommendationService = bursaryRecommendationService;
    }

    public CareerAdviceResponse careerAdviceForStudent(Principal principal) {
        StudentProfile profile = studentService.getProfileEntity(principal);
        return geminiService.getCareerAdvice(new CareerAdviceRequest(
                safe(profile.getQualificationLevel()),
                safe(profile.getInterests()),
                safe(profile.getSkills()),
                safe(profile.getLocation())
        ));
    }

    public List<BursaryResultDto> bursaryGuidanceForStudent(Principal principal) {
        return bursaryRecommendationService.recommendForStudent(principal);
    }

    public AiDashboardSummaryResponse dashboardSummary(Principal principal) {
        Map<String, Object> dashboard = studentService.dashboard(principal);
        CareerAdviceResponse careers = careerAdviceForStudent(principal);
        List<BursaryResultDto> bursaries = bursaryGuidanceForStudent(principal).stream().limit(5).toList();
        List<String> insights = List.of(
                "Saved opportunities and application progress are sourced from your EduRite profile.",
                bursaries.stream().anyMatch(BursaryResultDto::officialSource)
                        ? "Bursary guidance prioritised official provider records where available."
                        : "No official provider bursary was available, so trusted public fallback sources were used.",
                "Career guidance uses your stored profile and Gemini when live AI is configured."
        );
        return new AiDashboardSummaryResponse(dashboard, insights, bursaries, careers.recommendedCareers());
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "not provided" : value;
    }
}
