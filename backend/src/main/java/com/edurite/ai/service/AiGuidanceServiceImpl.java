package com.edurite.ai.service;

import com.edurite.ai.dto.AiGuidanceRequest;
import com.edurite.ai.dto.AiGuidanceResponse;
import com.edurite.ai.provider.AiTextProvider;
import com.edurite.recommendation.dto.RecommendationResultDto;
import com.edurite.recommendation.service.RecommendationService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentService;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
/**
 * Coordinates request validation, prompt generation, Gemini calling, and fallback handling.
 */
public class AiGuidanceServiceImpl implements AiGuidanceService {

    private static final Logger log = LoggerFactory.getLogger(AiGuidanceServiceImpl.class);

    private final AiPromptBuilder promptBuilder;
    private final AiTextProvider aiTextProvider;
    private final RecommendationService recommendationService;
    private final StudentService studentService;

    public AiGuidanceServiceImpl(
            AiPromptBuilder promptBuilder,
            AiTextProvider aiTextProvider,
            RecommendationService recommendationService,
            StudentService studentService
    ) {
        this.promptBuilder = promptBuilder;
        this.aiTextProvider = aiTextProvider;
        this.recommendationService = recommendationService;
        this.studentService = studentService;
    }

    @Override
    public AiGuidanceResponse generateGuidance(AiGuidanceRequest request, Principal principal) {
        String prompt = promptBuilder.buildGuidancePrompt(request);

        try {
            if (!aiTextProvider.isAvailable()) {
                return fallback(principal, "Gemini integration is disabled or unavailable.");
            }

            String modelOutput = aiTextProvider.generateText(prompt);
            if (!StringUtils.hasText(modelOutput)) {
                return fallback(principal, "Gemini returned an empty response.");
            }

            return new AiGuidanceResponse(modelOutput, false, "gemini", "AI guidance generated successfully.");
        } catch (Exception ex) {
            log.warn("AI guidance generation failed, using fallback: {}", ex.getMessage());
            return fallback(principal, "AI guidance is temporarily unavailable.");
        }
    }

    @Override
    public AiGuidanceResponse generateGuidanceFromProfile(Principal principal) {
        StudentProfile profile = studentService.getProfileEntity(principal);
        AiGuidanceRequest request = new AiGuidanceRequest(
                profile.getFirstName(),
                profile.getLastName(),
                profile.getInterests(),
                profile.getSkills(),
                profile.getQualificationLevel(),
                profile.getLocation(),
                profile.getBio(),
                profile.getCareerGoals(),
                profile.getQualifications(),
                profile.getExperience()
        );
        return generateGuidance(request, principal);
    }

    private AiGuidanceResponse fallback(Principal principal, String message) {
        RecommendationResultDto fallbackResult = recommendationService.generateForStudent(principal);
        String fallbackText = """
                {
                  "suggestedCareers": %s,
                  "bursaryCategories": %s,
                  "skillsToImprove": %s,
                  "coursesOrCertifications": %s,
                  "reasoningSummary": "Generated from the existing rule engine while AI is unavailable.",
                  "confidenceNote": "Fallback mode uses deterministic profile rules."
                }
                """.formatted(
                fallbackResult.suggestedCareers(),
                fallbackResult.suggestedBursaries(),
                fallbackResult.profileImprovementTips(),
                fallbackResult.suggestedCoursesOrImprovements()
        );

        return new AiGuidanceResponse(fallbackText, true, fallbackResult.modelVersion(), message);
    }
}
