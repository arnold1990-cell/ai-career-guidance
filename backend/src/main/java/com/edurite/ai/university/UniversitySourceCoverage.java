package com.edurite.ai.university;

import java.time.OffsetDateTime;
import java.util.Map;

public record UniversitySourceCoverage(
        int configuredUniversityCount,
        int activeUniversityCount,
        long storedPageCount,
        long successfulCrawlCount,
        long failedCrawlCount,
        OffsetDateTime lastCrawlTime,
        Map<String, Long> pagesPerUniversity
) {
}
