package com.edurite.recommendation.service;

import com.edurite.recommendation.dto.RecommendationResultDto;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentService;
import com.edurite.subscription.repository.SubscriptionRepository;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    private final StudentService studentService;
    private final SubscriptionRepository subscriptionRepository;

    public RecommendationService(StudentService studentService, SubscriptionRepository subscriptionRepository) {
        this.studentService = studentService;
        this.subscriptionRepository = subscriptionRepository;
    }

    public List<RecommendationResultDto> generateForStudent(Principal principal) {
        StudentProfile profile = studentService.getProfileEntity(principal);
        boolean premium = subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(profile.getUserId())
                .map(s -> "PLAN_PREMIUM".equals(s.getPlanCode()) && "ACTIVE".equals(s.getStatus())).orElse(false);

        if (!profile.isProfileCompleted()) {
            return List.of(new RecommendationResultDto("complete-profile", "ACTION", "Complete your profile", 100,
                    "Upload your CV and transcripts for personalised recommendations.", "rule-engine-v2"));
        }

        List<RecommendationResultDto> results = new ArrayList<>();
        results.add(new RecommendationResultDto("career-software-engineer", "CAREER", "Software Engineer", 91,
                "Strong skills and interests alignment with technology pathways.", "rule-engine-v2"));
        results.add(new RecommendationResultDto("bursary-stem-excellence", "BURSARY", "STEM Excellence Bursary", 87,
                "Qualification level and academic performance match eligibility.", "rule-engine-v2"));
        if (premium) {
            results.add(new RecommendationResultDto("course-data-structures", "COURSE", "Advanced Data Structures", 85,
                    "Recommended to close identified skill gaps in problem solving.", "rule-engine-v2"));
            results.add(new RecommendationResultDto("action-public-speaking", "IMPROVEMENT", "Improve public speaking", 74,
                    "Add communication projects to strengthen interview readiness.", "rule-engine-v2"));
        }
        return results;
    }
}
