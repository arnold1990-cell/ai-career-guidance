package com.edurite.recommendation.service; // declares the package path for this Java file

import com.edurite.recommendation.dto.RecommendationResultDto; // imports a class so it can be used in this file
import com.edurite.recommendation.dto.RecommendationItemDto; // imports a class so it can be used in this file
import com.edurite.student.entity.StudentProfile; // imports a class so it can be used in this file
import com.edurite.student.service.StudentService; // imports a class so it can be used in this file
import com.edurite.subscription.repository.SubscriptionRepository; // imports a class so it can be used in this file
import java.security.Principal; // imports a class so it can be used in this file
import java.util.ArrayList; // imports a class so it can be used in this file
import java.util.List; // imports a class so it can be used in this file
import java.util.Locale; // imports a class so it can be used in this file
import org.springframework.stereotype.Service; // imports a class so it can be used in this file

// @Service marks a class that contains business logic.
@Service // marks this class as a service containing business logic
/**
 * This class named RecommendationService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class RecommendationService { // defines a class type

    private final StudentService studentService; // executes this statement as part of the application logic
    private final SubscriptionRepository subscriptionRepository; // reads or writes data through the database layer

    public RecommendationService(StudentService studentService, SubscriptionRepository subscriptionRepository) { // reads or writes data through the database layer
        this.studentService = studentService; // executes this statement as part of the application logic
        this.subscriptionRepository = subscriptionRepository; // reads or writes data through the database layer
    } // ends the current code block

    /**
     * Note: this method handles the "generateForStudent" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public RecommendationResultDto generateForStudent(Principal principal) { // declares a method that defines behavior for this class
        StudentProfile profile = studentService.getProfileEntity(principal); // executes this statement as part of the application logic
        boolean premium = subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(profile.getUserId()) // reads or writes data through the database layer
                .map(s -> "PLAN_PREMIUM".equals(s.getPlanCode()) && "ACTIVE".equals(s.getStatus())).orElse(false); // executes this statement as part of the application logic

        List<String> skills = split(profile.getSkills()); // executes this statement as part of the application logic
        List<String> interests = split(profile.getInterests()); // executes this statement as part of the application logic
        String qualification = normalize(profile.getQualificationLevel()); // executes this statement as part of the application logic
        String experience = normalize(profile.getExperience()); // executes this statement as part of the application logic
        String goals = normalize(profile.getCareerGoals()); // executes this statement as part of the application logic

        List<RecommendationItemDto> suggestedCareers = new ArrayList<>(); // creates a new object instance and stores it in a variable
        List<RecommendationItemDto> suggestedBursaries = new ArrayList<>(); // creates a new object instance and stores it in a variable
        List<RecommendationItemDto> improvements = new ArrayList<>(); // creates a new object instance and stores it in a variable
        List<String> profileTips = new ArrayList<>(); // creates a new object instance and stores it in a variable

        if (containsAny(interests, "technology", "software", "computers") || containsAny(skills, "java", "programming", "coding")) { // checks a condition and runs this block only when true
            suggestedCareers.add(new RecommendationItemDto("career-software-engineer", "Software Engineer", 91, // supports the surrounding application logic
                    "Your interest in technology and software skills aligns well with engineering pathways.")); // executes this statement as part of the application logic
            suggestedBursaries.add(new RecommendationItemDto("bursary-stem-excellence", "STEM Excellence Bursary", 87, // supports the surrounding application logic
                    "This bursary supports high-potential STEM students and matches your profile signals.")); // executes this statement as part of the application logic
        } // ends the current code block

        if (containsAny(interests, "business", "finance") || containsAny(goals, "entrepreneur", "management")) { // checks a condition and runs this block only when true
            suggestedCareers.add(new RecommendationItemDto("career-business-analyst", "Business Analyst", 84, // supports the surrounding application logic
                    "Your profile suggests strong potential for data-informed business roles.")); // executes this statement as part of the application logic
        } // ends the current code block

        if (containsAny(interests, "health", "medical", "science")) { // checks a condition and runs this block only when true
            suggestedCareers.add(new RecommendationItemDto("career-health-data-analyst", "Health Data Analyst", 80, // supports the surrounding application logic
                    "You show science-focused interests that map to modern healthcare analytics careers.")); // executes this statement as part of the application logic
        } // ends the current code block

        if (qualification.isBlank()) { // checks a condition and runs this block only when true
            profileTips.add("Add your qualification level so eligibility matching can improve."); // executes this statement as part of the application logic
        } // ends the current code block
        if (skills.isEmpty()) { // checks a condition and runs this block only when true
            profileTips.add("List at least 3 key skills to improve career and course matching."); // executes this statement as part of the application logic
        } // ends the current code block
        if (interests.isEmpty()) { // checks a condition and runs this block only when true
            profileTips.add("Add your interests so recommendations can be tailored."); // executes this statement as part of the application logic
        } // ends the current code block
        if (profile.getCvFileUrl() == null) { // checks a condition and runs this block only when true
            profileTips.add("Upload your CV to unlock stronger matching confidence."); // executes this statement as part of the application logic
        } // ends the current code block

        improvements.add(new RecommendationItemDto("improvement-communication", "Strengthen communication portfolio", 76, // supports the surrounding application logic
                "Add project presentations or teamwork examples to improve employability.")); // executes this statement as part of the application logic

        if (premium) { // checks a condition and runs this block only when true
            improvements.add(new RecommendationItemDto("course-data-structures", "Advanced Data Structures", 85, // supports the surrounding application logic
                    "Recommended to close analytical problem-solving skill gaps.")); // executes this statement as part of the application logic
        } else { // supports the surrounding application logic
            improvements.add(new RecommendationItemDto("upgrade-premium", "Upgrade to Premium for deeper guidance", 70, // supports the surrounding application logic
                    "Premium unlocks additional course-level recommendations and insights.")); // executes this statement as part of the application logic
        } // ends the current code block

        if (suggestedCareers.isEmpty()) { // checks a condition and runs this block only when true
            suggestedCareers.add(new RecommendationItemDto("career-generalist", "Digital Operations Specialist", 72, // supports the surrounding application logic
                    "A versatile path while your profile becomes more complete.")); // executes this statement as part of the application logic
        } // ends the current code block
        if (suggestedBursaries.isEmpty()) { // checks a condition and runs this block only when true
            suggestedBursaries.add(new RecommendationItemDto("bursary-career-growth", "Career Growth Support Bursary", 74, // supports the surrounding application logic
                    "A broad bursary option suited to developing profiles.")); // executes this statement as part of the application logic
        } // ends the current code block

        if (!profile.isProfileCompleted()) { // checks a condition and runs this block only when true
            profileTips.add(0, "Complete your student profile and upload required documents for better recommendations."); // executes this statement as part of the application logic
        } // ends the current code block
        if (experience.isBlank()) { // checks a condition and runs this block only when true
            profileTips.add("Add internships, volunteer work, or projects under experience."); // executes this statement as part of the application logic
        } // ends the current code block

        return new RecommendationResultDto(suggestedCareers, suggestedBursaries, improvements, profileTips, "rule-engine-v3"); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "split" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private List<String> split(String input) { // declares a method that defines behavior for this class
        if (input == null || input.isBlank()) { // checks a condition and runs this block only when true
            return List.of(); // returns a value from this method to the caller
        } // ends the current code block
        return List.of(input.split(",")).stream().map(this::normalize).filter(s -> !s.isBlank()).toList(); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "normalize" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private String normalize(String value) { // declares a method that defines behavior for this class
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim(); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "containsAny" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private boolean containsAny(List<String> values, String... keywords) { // declares a method that defines behavior for this class
        for (String value : values) { // loops through items or numbers repeatedly
            for (String keyword : keywords) { // loops through items or numbers repeatedly
                if (value.contains(keyword)) { // checks a condition and runs this block only when true
                    return true; // returns a value from this method to the caller
                } // ends the current code block
            } // ends the current code block
        } // ends the current code block
        return false; // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "containsAny" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private boolean containsAny(String value, String... keywords) { // declares a method that defines behavior for this class
        for (String keyword : keywords) { // loops through items or numbers repeatedly
            if (value.contains(keyword)) { // checks a condition and runs this block only when true
                return true; // returns a value from this method to the caller
            } // ends the current code block
        } // ends the current code block
        return false; // returns a value from this method to the caller
    } // ends the current code block
} // ends the current code block
