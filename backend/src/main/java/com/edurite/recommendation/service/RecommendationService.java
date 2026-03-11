package com.edurite.recommendation.service;

import com.edurite.recommendation.dto.RecommendationResultDto;
import com.edurite.recommendation.dto.RecommendationItemDto;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentService;
import com.edurite.subscription.repository.SubscriptionRepository;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

// @Service marks a class that contains business logic.
@Service
/**
 * This class named RecommendationService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class RecommendationService {

    private final StudentService studentService;
    private final SubscriptionRepository subscriptionRepository;

    public RecommendationService(StudentService studentService, SubscriptionRepository subscriptionRepository) {
        this.studentService = studentService;
        this.subscriptionRepository = subscriptionRepository;
    }

    /**
     * Beginner note: this method handles the "generateForStudent" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public RecommendationResultDto generateForStudent(Principal principal) {
        StudentProfile profile = studentService.getProfileEntity(principal);
        boolean premium = subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(profile.getUserId())
                .map(s -> "PLAN_PREMIUM".equals(s.getPlanCode()) && "ACTIVE".equals(s.getStatus())).orElse(false);

        List<String> skills = split(profile.getSkills());
        List<String> interests = split(profile.getInterests());
        String qualification = normalize(profile.getQualificationLevel());
        String experience = normalize(profile.getExperience());
        String goals = normalize(profile.getCareerGoals());

        List<RecommendationItemDto> suggestedCareers = new ArrayList<>();
        List<RecommendationItemDto> suggestedBursaries = new ArrayList<>();
        List<RecommendationItemDto> improvements = new ArrayList<>();
        List<String> profileTips = new ArrayList<>();

        if (containsAny(interests, "technology", "software", "computers") || containsAny(skills, "java", "programming", "coding")) {
            suggestedCareers.add(new RecommendationItemDto("career-software-engineer", "Software Engineer", 91,
                    "Your interest in technology and software skills aligns well with engineering pathways."));
            suggestedBursaries.add(new RecommendationItemDto("bursary-stem-excellence", "STEM Excellence Bursary", 87,
                    "This bursary supports high-potential STEM students and matches your profile signals."));
        }

        if (containsAny(interests, "business", "finance") || containsAny(goals, "entrepreneur", "management")) {
            suggestedCareers.add(new RecommendationItemDto("career-business-analyst", "Business Analyst", 84,
                    "Your profile suggests strong potential for data-informed business roles."));
        }

        if (containsAny(interests, "health", "medical", "science")) {
            suggestedCareers.add(new RecommendationItemDto("career-health-data-analyst", "Health Data Analyst", 80,
                    "You show science-focused interests that map to modern healthcare analytics careers."));
        }

        if (qualification.isBlank()) {
            profileTips.add("Add your qualification level so eligibility matching can improve.");
        }
        if (skills.isEmpty()) {
            profileTips.add("List at least 3 key skills to improve career and course matching.");
        }
        if (interests.isEmpty()) {
            profileTips.add("Add your interests so recommendations can be tailored.");
        }
        if (profile.getCvFileUrl() == null) {
            profileTips.add("Upload your CV to unlock stronger matching confidence.");
        }

        improvements.add(new RecommendationItemDto("improvement-communication", "Strengthen communication portfolio", 76,
                "Add project presentations or teamwork examples to improve employability."));

        if (premium) {
            improvements.add(new RecommendationItemDto("course-data-structures", "Advanced Data Structures", 85,
                    "Recommended to close analytical problem-solving skill gaps."));
        } else {
            improvements.add(new RecommendationItemDto("upgrade-premium", "Upgrade to Premium for deeper guidance", 70,
                    "Premium unlocks additional course-level recommendations and insights."));
        }

        if (suggestedCareers.isEmpty()) {
            suggestedCareers.add(new RecommendationItemDto("career-generalist", "Digital Operations Specialist", 72,
                    "A versatile path while your profile becomes more complete."));
        }
        if (suggestedBursaries.isEmpty()) {
            suggestedBursaries.add(new RecommendationItemDto("bursary-career-growth", "Career Growth Support Bursary", 74,
                    "A broad bursary option suited to developing profiles."));
        }

        if (!profile.isProfileCompleted()) {
            profileTips.add(0, "Complete your student profile and upload required documents for better recommendations.");
        }
        if (experience.isBlank()) {
            profileTips.add("Add internships, volunteer work, or projects under experience.");
        }

        return new RecommendationResultDto(suggestedCareers, suggestedBursaries, improvements, profileTips, "rule-engine-v3");
    }

    /**
     * Beginner note: this method handles the "split" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private List<String> split(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }
        return List.of(input.split(",")).stream().map(this::normalize).filter(s -> !s.isBlank()).toList();
    }

    /**
     * Beginner note: this method handles the "normalize" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    /**
     * Beginner note: this method handles the "containsAny" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private boolean containsAny(List<String> values, String... keywords) {
        for (String value : values) {
            for (String keyword : keywords) {
                if (value.contains(keyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Beginner note: this method handles the "containsAny" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
