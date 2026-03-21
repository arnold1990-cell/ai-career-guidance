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
    void enrichesProgrammesWithCleanerDiagnosticsAndCoverageMetrics() {
        StudentProfile profile = new StudentProfile();
        profile.setInterests("software engineering");
        profile.setSkills("problem solving");

        UniversitySourcesAnalysisRequest request = new UniversitySourcesAnalysisRequest(List.of(), "Computer Science", "Software Developer", "Undergraduate", 5);
        UniversitySourcesAnalysisResponse response = new UniversitySourcesAnalysisResponse(
                true, false, "live Gemini", "PARTIALLY_GROUNDED", 50, null,
                List.of("https://www.uj.ac.za/programmes/computer-science", "https://www.unisa.ac.za/admissions/it", "https://www.uct.ac.za/programmes/bsc"),
                List.of("https://www.uj.ac.za/programmes/computer-science", "https://www.unisa.ac.za/admissions/it", "https://www.uct.ac.za/programmes/bsc"),
                List.of("https://www.uj.ac.za/programmes/computer-science"),
                List.of("https://www.unisa.ac.za/admissions/it", "https://www.uct.ac.za/programmes/bsc"),
                1, "summary", List.of("inferred"),
                List.of(new UniversitySourcesAnalysisResponse.RecommendedCareer("Software Developer", "Strong fit", List.of("Mathematics"), List.of("BSc Computer Science"))),
                List.of(new UniversitySourcesAnalysisResponse.RecommendedProgramme(
                        "BSc Computer Science", "University of Johannesburg",
                        List.of("Mathematics required", "Mathematics required", "Not found in fetched sources"),
                        "Official source mentions entry requirements."
                )),
                List.of(), List.of("University of Johannesburg"), List.of("Grade 12 passes"), List.of("Mathematics required"),
                List.of(), List.of(), List.of(), 78, "gemini"
        );

        List<UniversitySourcePageResult> pages = List.of(
                new UniversitySourcePageResult(
                        "https://www.uj.ac.za/programmes/computer-science", "Computer Science", UniversityPageType.PROGRAMME_DETAIL,
                        "Computer Science admission requirements mathematics English APS details are published here.",
                        Set.of("computer science", "mathematics"), List.of("BSc Computer Science", "Admission requirements"), true, null, null
                ),
                new UniversitySourcePageResult(
                        "https://www.unisa.ac.za/admissions/it", "IT admissions", UniversityPageType.ADMISSIONS_OVERVIEW,
                        "", Set.of(), List.of(), false, "timeout", UniversityCrawlFailureType.TIMEOUT
                ),
                new UniversitySourcePageResult(
                        "https://www.uct.ac.za/programmes/bsc", "Portal", UniversityPageType.UNKNOWN,
                        "sign in with password", Set.of(), List.of(), false, "blocked", UniversityCrawlFailureType.PROTECTED
                )
        );

        UniversitySourcesAnalysisResponse enriched = enricher.enrich(response, request, profile, response.sourceUrls(), pages);

        assertThat(enriched.recommendedProgrammes()).hasSize(1);
        assertThat(enriched.recommendedProgrammes().get(0).admissionRequirements()).containsExactly("Mathematics required", "Not found in fetched sources");
        assertThat(enriched.sourceDiagnostics()).hasSize(3);
        assertThat(enriched.sourceDiagnostics().get(1).fetchStatus()).isEqualTo("TIMEOUT");
        assertThat(enriched.sourceDiagnostics().get(1).failureReason()).isEqualTo("Official source took too long to respond.");
        assertThat(enriched.sourceDiagnostics().get(2).fetchStatus()).isEqualTo("PROTECTED");
        assertThat(enriched.sourceCoverage().attemptedSourcesCount()).isEqualTo(3);
        assertThat(enriched.sourceCoverage().usableSourcesCount()).isEqualTo(1);
        assertThat(enriched.sourceCoverage().protectedSourcesCount()).isEqualTo(1);
        assertThat(enriched.sourceCoverage().timeoutSourcesCount()).isEqualTo(1);
        assertThat(enriched.suitabilityScoreReason()).contains("Suitability score 78");
    }
}
