package com.edurite.ai.service;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.university.UniversityPageType;
import com.edurite.ai.university.UniversitySourcePageResult;
import com.edurite.ai.university.UniversitySourceRegistryService;
import com.edurite.student.entity.StudentProfile;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UniversityStructuredRecommendationServiceTest {

    @Test
    void broadEngineeringInterestDoesNotOverfitToSingleNiche() {
        UniversitySourceRegistryService registryService = mock(UniversitySourceRegistryService.class);
        when(registryService.inferInstitutionName("https://www.cput.ac.za/study/engineering")).thenReturn("Cape Peninsula University of Technology");
        UniversityStructuredRecommendationService service = new UniversityStructuredRecommendationService(registryService);

        StudentProfile profile = new StudentProfile();
        profile.setInterests("Engineering");
        UniversitySourcesAnalysisRequest request = new UniversitySourcesAnalysisRequest(List.of(), "Engineering", "Engineering", "Undergraduate", 6);
        List<UniversitySourcePageResult> pages = List.of(
                new UniversitySourcePageResult(
                        "https://www.cput.ac.za/study/engineering",
                        "Faculty of Engineering and the Built Environment",
                        UniversityPageType.PROGRAMME_DETAIL,
                        "Civil engineering and mechanical engineering programmes require mathematics, english and physical sciences. Duration 4 years.",
                        Set.of("engineering", "civil engineering", "mechanical engineering"),
                        List.of("Engineering programmes"),
                        true,
                        null,
                        null
                )
        );

        UniversitySourcesAnalysisResponse response = service.buildResponse(
                request,
                profile,
                List.of("https://www.cput.ac.za/study/engineering"),
                pages,
                new UniversitySourcesAnalysisResponse.AnalysisDiagnostics(1, 1, 1, 1, 0, 0, 0, 0, 25L, 12L),
                "SUCCESS",
                "PARTIAL",
                "ok"
        );

        assertThat(response.recommendedCareers()).extracting(UniversitySourcesAnalysisResponse.RecommendedCareer::name)
                .anySatisfy(name -> assertThat(name).contains("Civil"))
                .anySatisfy(name -> assertThat(name).contains("Mechanical"));
        assertThat(response.recommendedProgrammes()).isNotEmpty();
    }
}
