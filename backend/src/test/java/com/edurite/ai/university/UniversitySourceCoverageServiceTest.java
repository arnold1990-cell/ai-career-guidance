package com.edurite.ai.university;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniversitySourceCoverageServiceTest {

    @Mock
    private UniversitySourceRegistryService registryService;
    @Mock
    private CrawledUniversityPageRepository repository;

    @Test
    void reportsPagesPerUniversityInCoverageStats() {
        UniversitySourceCoverageService service = new UniversitySourceCoverageService(registryService, repository);

        when(registryService.configuredUniversityCount()).thenReturn(55);
        when(registryService.getActiveUniversities()).thenReturn(List.of(
                new UniversityRegistryProperties.UniversityRegistryEntry(),
                new UniversityRegistryProperties.UniversityRegistryEntry()
        ));
        when(repository.count()).thenReturn(100L);
        when(repository.countByCrawlStatus(CrawlStatus.SUCCESS)).thenReturn(90L);
        when(repository.countByCrawlStatus(CrawlStatus.FAILED)).thenReturn(10L);
        when(repository.findAll()).thenReturn(List.of());
        when(repository.countActiveSuccessfulPagesByUniversity()).thenReturn(List.of(
                pageCount("University A", 40),
                pageCount("University B", 50)
        ));

        UniversitySourceCoverage coverage = service.getCoverage();

        assertThat(coverage.configuredUniversityCount()).isEqualTo(55);
        assertThat(coverage.pagesPerUniversity()).containsEntry("University A", 40L).containsEntry("University B", 50L);
    }

    private CrawledUniversityPageRepository.UniversityPageCountView pageCount(String university, long count) {
        return new CrawledUniversityPageRepository.UniversityPageCountView() {
            @Override
            public String getUniversityName() {
                return university;
            }

            @Override
            public long getPageCount() {
                return count;
            }
        };
    }
}
