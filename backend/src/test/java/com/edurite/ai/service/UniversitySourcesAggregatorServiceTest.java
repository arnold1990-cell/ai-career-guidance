package com.edurite.ai.service;

import com.edurite.ai.dto.FetchedUniversityPage;
import com.edurite.ai.dto.UniversityPageType;
import com.edurite.student.entity.StudentProfile;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UniversitySourcesAggregatorServiceTest {

    private final UniversitySourcesAggregatorService service = new UniversitySourcesAggregatorService();

    @Test
    void aggregatesSuccessfulPagesAndKeepsFailedUrls() {
        var page1 = new FetchedUniversityPage("https://www.unisa.ac.za/a", "Computer Science", UniversityPageType.QUALIFICATION_LIST,
                "Computer Science undergraduate requirements and modules", List.of("Computer Science"), List.of(), true, null);
        var page2 = new FetchedUniversityPage("https://www.uj.ac.za/b", null, UniversityPageType.UNKNOWN,
                "", List.of(), List.of(), false, "Failed");

        StudentProfile profile = new StudentProfile();
        profile.setInterests("software development");
        profile.setSkills("java");

        var result = service.aggregate(List.of(page1, page2), profile, "Software Engineering", "Developer", "Grade 12");

        assertThat(result.successfulUrls()).containsExactly("https://www.unisa.ac.za/a");
        assertThat(result.failedUrls()).containsExactly("https://www.uj.ac.za/b");
        assertThat(result.extractedKeywords()).contains("Computer Science");
        assertThat(result.mergedAcademicContext()).contains("Computer Science");
    }
}
