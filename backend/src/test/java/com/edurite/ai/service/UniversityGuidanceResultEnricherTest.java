package com.edurite.ai.service;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.university.UniversityCrawlFailureType;
import com.edurite.ai.university.UniversityPageType;
import com.edurite.ai.university.UniversitySourcePageResult;
import com.edurite.student.entity.StudentProfile;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UniversityGuidanceResultEnricherTest {

    private final UniversityGuidanceResultEnricher enricher = new UniversityGuidanceResultEnricher();

    @Test
    void enrichesProgrammesWithDiagnosticsAndDeduplicatedRequirements() {
        StudentProfile profile = new StudentProfile();
        profile.setInterests("software engineering");
        profile.setSkills("problem solving");

        UniversitySourcesAnalysisRequest request = new UniversitySourcesAnalysisRequest(List.of(), "Computer Science", "Software Developer", "Undergraduate", 5);
        UniversitySourcesAnalysisResponse response = new UniversitySourcesAnalysisResponse(
                true,
                false,
                "PARTIAL",
                "PARTIAL",
                "PARTIALLY_GROUNDED",
                50,
                null,
                List.of("https://www.uj.ac.za/programmes/computer-science", "https://www.unisa.ac.za/admissions/it"),
                List.of("https://www.uj.ac.za/programmes/computer-science", "https://www.unisa.ac.za/admissions/it"),
                List.of("https://www.uj.ac.za/programmes/computer-science"),
                List.of("https://www.unisa.ac.za/admissions/it"),
                1,
                "summary",
                List.of("inferred"),
                List.of(new UniversitySourcesAnalysisResponse.RecommendedCareer("Software Developer", "Strong fit", List.of("Mathematics"), List.of("BSc Computer Science"))),
                List.of(new UniversitySourcesAnalysisResponse.RecommendedProgramme(
                        "BSc Computer Science",
                        "University of Johannesburg",
                        List.of("Mathematics required", "Mathematics required", "Not found in fetched sources"),
                        "Official source mentions entry requirements."
                )),
                List.of(),
                List.of("University of Johannesburg"),
                List.of("Grade 12 passes"),
                List.of("Mathematics required"),
                List.of(),
                List.of(),
                List.of(),
                78,
                "gemini"
        );

        List<UniversitySourcePageResult> pages = List.of(
                new UniversitySourcePageResult(
                        "https://www.uj.ac.za/programmes/computer-science",
                        "Computer Science",
                        UniversityPageType.PROGRAMME_DETAIL,
                        "Computer Science admission requirements mathematics English APS details are published here.",
                        Set.of("computer science", "mathematics"),
                        List.of("BSc Computer Science", "Admission requirements"),
                        true,
                        null,
                        null
                ),
                new UniversitySourcePageResult(
                        "https://www.unisa.ac.za/admissions/it",
                        "IT admissions",
                        UniversityPageType.ADMISSIONS_OVERVIEW,
                        "",
                        Set.of(),
                        List.of(),
                        false,
                        "timeout",
                        UniversityCrawlFailureType.TIMEOUT
                )
        );

        UniversitySourcesAnalysisResponse enriched = enricher.enrich(response, request, profile, response.sourceUrls(), pages);

        assertThat(enriched.recommendedProgrammes()).hasSize(1);
        assertThat(enriched.recommendedProgrammes().get(0).admissionRequirements()).containsExactly("Mathematics required", "Not found in fetched sources");
        assertThat(enriched.recommendedProgrammes().get(0).verifiedFacts()).isNotEmpty();
        assertThat(enriched.recommendedProgrammes().get(0).missingData()).contains("Deadline missing from fetched sources.");
        assertThat(enriched.sourceDiagnostics()).hasSize(2);
        assertThat(enriched.sourceDiagnostics().get(1).fetchStatus()).isEqualTo("TIMEOUT");
        assertThat(enriched.sourceCoverage()).isNotNull();
        assertThat(enriched.suitabilityScoreReason()).contains("Suitability score 78");
    }

    @Test
    void ranksProgrammesAcrossMultipleUniversitiesAndMarksPartialSourceStatus() {
        StudentProfile profile = new StudentProfile();
        profile.setInterests("software engineering data science");
        profile.setSkills("mathematics python");

        UniversitySourcesAnalysisRequest request = new UniversitySourcesAnalysisRequest(List.of(), "Computer Science", "Software Engineer", "Undergraduate", 5);
        UniversitySourcesAnalysisResponse response = new UniversitySourcesAnalysisResponse(
                true, false, "SUCCESS", "LIVE", "FULLY_GROUNDED", 80, null,
                List.of("https://www.uj.ac.za/programmes/cs", "https://www.uct.ac.za/apply/cs"),
                List.of("https://www.uj.ac.za/programmes/cs", "https://www.uct.ac.za/apply/cs"),
                List.of("https://www.uj.ac.za/programmes/cs", "https://www.uct.ac.za/apply/cs"),
                List.of(), 2,
                "Based on your profile and current guidance context.", List.of(), List.of(),
                List.of(
                        new UniversitySourcesAnalysisResponse.RecommendedProgramme("BSc Computer Science", "University of Cape Town", List.of("Mathematics 70%"), "Strong theory and systems focus."),
                        new UniversitySourcesAnalysisResponse.RecommendedProgramme("Diploma in IT", "University of Johannesburg", List.of("Mathematics 60%", "Deadline not found in fetched sources"), "Applied software pathway.")
                ),
                List.of(), List.of("University of Cape Town", "University of Johannesburg"), List.of(), List.of(), List.of(), List.of(), List.of(), 82, "gemini"
        );

        List<UniversitySourcePageResult> pages = List.of(
                new UniversitySourcePageResult("https://www.uct.ac.za/apply/cs", "Computer Science", UniversityPageType.PROGRAMME_DETAIL,
                        "Computer Science admission requirements include mathematics and application dates.", Set.of("computer science", "mathematics"), List.of("BSc Computer Science", "Admission requirements"), true, null, null),
                new UniversitySourcePageResult("https://www.uj.ac.za/programmes/cs", "Diploma in IT", UniversityPageType.QUALIFICATION_LIST,
                        "Diploma in IT has mathematics requirements but no APS detail on this page.", Set.of("information technology", "mathematics"), List.of("Diploma in IT"), true, null, null)
        );

        UniversitySourcesAnalysisResponse enriched = enricher.enrich(response, request, profile, response.sourceUrls(), pages);

        assertThat(enriched.recommendedProgrammes()).hasSize(2);
        assertThat(enriched.recommendedProgrammes().get(0).name()).isEqualTo("BSc Computer Science");
        assertThat(enriched.recommendedProgrammes().get(0).rankingCategory()).isEqualTo("Best match");
        assertThat(enriched.recommendedProgrammes().get(1).sourceStatus()).isEqualTo("PARTIAL");
        assertThat(enriched.summary()).contains("Software Engineer");
    }

}
