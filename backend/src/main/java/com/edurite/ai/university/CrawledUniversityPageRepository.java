package com.edurite.ai.university;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CrawledUniversityPageRepository extends JpaRepository<CrawledUniversityPage, java.util.UUID> {

    Optional<CrawledUniversityPage> findBySourceUrl(String sourceUrl);

    List<CrawledUniversityPage> findByActiveTrueAndCrawlStatus(CrawlStatus crawlStatus);

    List<CrawledUniversityPage> findByActiveTrueAndCrawlStatus(CrawlStatus crawlStatus, Pageable pageable);

    long countByCrawlStatus(CrawlStatus crawlStatus);

    long countByUniversityNameAndActiveTrueAndCrawlStatus(String universityName, CrawlStatus crawlStatus);

    @Query("""
            select c.universityName as universityName, count(c) as pageCount
            from CrawledUniversityPage c
            where c.active = true and c.crawlStatus = com.edurite.ai.university.CrawlStatus.SUCCESS
            group by c.universityName
            """)
    List<UniversityPageCountView> countActiveSuccessfulPagesByUniversity();

    interface UniversityPageCountView {
        String getUniversityName();
        long getPageCount();
    }
}
