package com.edurite.ai.service;

import com.edurite.student.entity.StudentProfile;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UniversityGuidancePromptBuilder {

    public String buildPrompt(StudentProfile profile,
                              UniversitySourcesAggregatorService.AggregatedUniversityContext context,
                              String targetProgram,
                              String careerInterest,
                              String qualificationLevel,
                              int maxRecommendations,
                              List<String> seedCareers) {
        return """
                You are EduRite's academic and career guidance assistant.
                Return ONLY valid JSON with this schema:
                {
                  "summary": "string",
                  "recommendedCareers": ["string"],
                  "recommendedProgrammes": ["string"],
                  "recommendedUniversities": ["string"],
                  "keyRequirements": ["string"],
                  "skillGaps": ["string"],
                  "recommendedNextSteps": ["string"],
                  "warnings": ["string"],
                  "suitabilityScore": 0
                }

                Rules:
                - Recommend at least %d careers and at least %d programmes if enough evidence exists.
                - Be broad but grounded in the provided sources.
                - If sources are generic/list pages, explain the limitation in warnings.
                - Do not hallucinate programme-specific requirements that are not present.
                - Keep language practical and student-friendly.
                - suitabilityScore must be an integer between 0 and 100.

                Student profile:
                firstName: %s
                lastName: %s
                qualificationLevel: %s
                targetProgram: %s
                careerInterest: %s
                interests: %s
                skills: %s
                experience: %s
                location: %s
                transcriptUploaded: %s
                cvUploaded: %s

                Internal seed careers for breadth:
                %s

                Extracted source keywords:
                %s

                Academic source context:
                %s
                """.formatted(
                maxRecommendations,
                maxRecommendations,
                safe(profile.getFirstName()),
                safe(profile.getLastName()),
                safe(qualificationLevel, profile.getQualificationLevel()),
                safe(targetProgram),
                safe(careerInterest),
                safe(profile.getInterests()),
                safe(profile.getSkills()),
                safe(profile.getExperience()),
                safe(profile.getLocation()),
                yesNo(profile.getTranscriptFileUrl()),
                yesNo(profile.getCvFileUrl()),
                String.join(", ", seedCareers),
                String.join(", ", context.extractedKeywords()),
                context.mergedAcademicContext()
        );
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Not provided" : value.trim();
    }

    private String safe(String explicit, String fallback) {
        if (explicit != null && !explicit.isBlank()) {
            return explicit.trim();
        }
        return safe(fallback);
    }

    private String yesNo(String url) {
        return url == null || url.isBlank() ? "No" : "Yes";
    }
}
