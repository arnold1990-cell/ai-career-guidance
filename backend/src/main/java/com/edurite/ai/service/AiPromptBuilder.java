package com.edurite.ai.service;

import com.edurite.ai.dto.AiGuidanceRequest;
import org.springframework.stereotype.Component;

@Component
/**
 * Builds strict and reusable prompts for Gemini.
 *
 * Keeping prompt construction here keeps service orchestration easy to read and test.
 */
public class AiPromptBuilder {

    public String buildGuidancePrompt(AiGuidanceRequest request) {
        String template = """
                You are an Edurite career guidance assistant for students.
                Follow these rules strictly:
                1) Be practical, student-friendly, and concise.
                2) If exact bursaries are unknown, provide bursary categories only.
                3) Do not hallucinate or invent official bursary names.
                4) Keep recommendations grounded in the profile data below.
                5) Return output in JSON format with these top-level keys:
                   suggestedCareers, bursaryCategories, skillsToImprove,
                   coursesOrCertifications, reasoningSummary, confidenceNote.

                Student profile:
                - First name: %s
                - Last name: %s
                - Interests: %s
                - Skills: %s
                - Qualification level: %s
                - Location: %s
                - Bio: %s
                - Career goals: %s
                - Academic strengths: %s
                - Weak areas: %s

                JSON formatting requirements:
                - Each list item must contain title and shortExplanation fields.
                - Keep 3 to 5 items per list where possible.
                - Keep confidenceNote honest and mention uncertainty when data is sparse.
                """;

        return template.formatted(
                safe(request.firstName()),
                safe(request.lastName()),
                safe(request.interests()),
                safe(request.skills()),
                safe(request.qualificationLevel()),
                safe(request.location()),
                safe(request.bio()),
                safe(request.careerGoals()),
                safe(request.academicStrengths()),
                safe(request.weakAreas())
        );
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Not provided" : value.trim();
    }
}
