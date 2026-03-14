package com.edurite.ai.service;

import com.edurite.ai.dto.UniversityPageType;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UniversityPageClassifierTest {

    private final UniversityPageClassifier classifier = new UniversityPageClassifier();

    @Test
    void classifiesQualificationListAndExtractsKeywordsFromFixture() throws Exception {
        String html = Files.readString(Path.of("src/test/resources/fixtures/ai/unisa-undergrad.html"));
        UniversityPageType type = classifier.classify("UNISA Undergraduate qualifications", html);

        assertThat(type).isEqualTo(UniversityPageType.QUALIFICATION_LIST);
        assertThat(classifier.extractKeywords("UNISA Undergraduate qualifications", html))
                .contains("Computer Science", "Information Systems", "Accounting", "Entry requirements");
    }

    @Test
    void detectsAdmissionsOverview() {
        assertThat(classifier.classify("Apply for admission", "Application requirements and APS"))
                .isEqualTo(UniversityPageType.ADMISSIONS_OVERVIEW);
    }
}
