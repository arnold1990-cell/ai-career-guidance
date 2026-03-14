package com.edurite.ai.service;

import com.edurite.student.entity.StudentProfile;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UniversityGuidancePromptBuilderTest {

    private final UniversityGuidancePromptBuilder builder = new UniversityGuidancePromptBuilder();

    @Test
    void promptIncludesStudentAndRecommendationTargets() {
        StudentProfile profile = new StudentProfile();
        profile.setFirstName("Ada");
        profile.setQualificationLevel("Grade 12");
        profile.setInterests("Technology");

        var context = new UniversitySourcesAggregatorService.AggregatedUniversityContext(
                "[Source] undergraduate computer science",
                List.of("Computer Science"),
                List.of(),
                List.of("https://www.unisa.ac.za/x"),
                List.of()
        );

        String prompt = builder.buildPrompt(profile, context, "Software Engineering", "Developer", "Grade 12", 10,
                List.of("Software Developer"));

        assertThat(prompt).contains("at least 10 careers");
        assertThat(prompt).contains("Ada");
        assertThat(prompt).contains("Software Engineering");
        assertThat(prompt).contains("Computer Science");
    }
}
