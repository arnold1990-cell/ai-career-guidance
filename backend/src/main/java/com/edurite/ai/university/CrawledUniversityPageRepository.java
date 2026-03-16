package com.edurite.ai.university;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawledUniversityPageRepository extends JpaRepository<CrawledUniversityPage, java.util.UUID> {

    Optional<CrawledUniversityPage> findBySourceUrl(String sourceUrl);

    List<CrawledUniversityPage> findByActiveTrueAndCrawlStatus(CrawlStatus crawlStatus);

    List<CrawledUniversityPage> findByActiveTrueAndCrawlStatusAndUniversityNameIn(CrawlStatus crawlStatus, Set<String> universityNames);

    long countByCrawlStatus(CrawlStatus crawlStatus);
}
