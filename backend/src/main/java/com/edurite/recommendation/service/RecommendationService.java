package com.edurite.recommendation.service;

import com.edurite.recommendation.dto.RecommendationResultDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    public List<RecommendationResultDto> generateForStudent(String studentRef) {
        return List.of(
                new RecommendationResultDto("CAREER", "software-engineer", 0.92, "Strong math and science performance", "rule-engine-v1"),
                new RecommendationResultDto("BURSARY", "stem-excellence", 0.88, "Matches merit and financial criteria", "rule-engine-v1"),
                new RecommendationResultDto("COURSE", "bsc-computer-science", 0.86, "Aligned to interests in technology", "rule-engine-v1")
        );
    }
}
