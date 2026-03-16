package com.edurite.ai.service;

import com.edurite.ai.dto.BookwormChatRequest;
import com.edurite.ai.dto.BookwormChatResponse;
import com.edurite.bursary.service.BursaryRecommendationService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentService;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class BookwormService {

    private final StudentService studentService;
    private final UniversityContextService universityContextService;
    private final BursaryRecommendationService bursaryRecommendationService;

    public BookwormService(StudentService studentService,
                           UniversityContextService universityContextService,
                           BursaryRecommendationService bursaryRecommendationService) {
        this.studentService = studentService;
        this.universityContextService = universityContextService;
        this.bursaryRecommendationService = bursaryRecommendationService;
    }

    public BookwormChatResponse chat(Principal principal, BookwormChatRequest request) {
        StudentProfile profile = studentService.getProfileEntity(principal);
        UniversityContextService.UniversityContextResult universityContext = universityContextService
                .buildContext(profile, request.message(), 6);

        List<String> programmes = universityContext.programmesByUniversity().values().stream()
                .flatMap(List::stream)
                .distinct()
                .limit(8)
                .toList();

        List<String> careers = deriveCareers(profile, request.message());
        List<String> bursaries = bursaryRecommendationService.recommendForStudent(principal).stream()
                .map(item -> item.title() + " - " + item.provider())
                .limit(4)
                .toList();

        List<BookwormChatResponse.RecommendedUniversity> universities = universityContext.universities().stream()
                .map(item -> new BookwormChatResponse.RecommendedUniversity(
                        item.name(),
                        item.location(),
                        item.officialWebsite(),
                        item.category(),
                        item.programmes(),
                        item.entryRequirements(),
                        item.source()))
                .toList();

        List<String> websites = universities.stream()
                .map(BookwormChatResponse.RecommendedUniversity::officialWebsite)
                .filter(url -> url != null && !url.isBlank())
                .distinct()
                .toList();

        List<String> warnings = universities.isEmpty()
                ? List.of("No direct university match found in the internal module. Try a more specific programme or career keyword.")
                : List.of();

        return new BookwormChatResponse(
                buildAnswerText(profile, request.message(), careers, programmes, universities),
                careers,
                programmes,
                universities,
                websites,
                bursaries,
                buildRoadmap(careers, programmes, universities),
                warnings,
                "internal_university_module"
        );
    }

    public List<String> suggestions() {
        return List.of(
                "What can I study with Maths and Physics?",
                "Which universities offer Computer Science?",
                "Show me bursaries for Engineering",
                "Give me a roadmap to become a Data Scientist"
        );
    }

    private List<String> deriveCareers(StudentProfile profile, String message) {
        String haystack = (safe(message) + " " + safe(profile.getInterests()) + " " + safe(profile.getSkills())).toLowerCase(Locale.ROOT);
        if (haystack.contains("data")) {
            return List.of("Data Scientist", "Data Analyst", "Machine Learning Engineer");
        }
        if (haystack.contains("engineering") || haystack.contains("physics")) {
            return List.of("Electrical Engineer", "Civil Engineer", "Mechanical Engineer");
        }
        if (haystack.contains("medicine") || haystack.contains("health")) {
            return List.of("Doctor", "Pharmacist", "Biokineticist");
        }
        return List.of("Software Developer", "Business Analyst", "Project Manager");
    }

    private List<String> buildRoadmap(List<String> careers,
                                      List<String> programmes,
                                      List<BookwormChatResponse.RecommendedUniversity> universities) {
        String career = careers.isEmpty() ? "your chosen career" : careers.get(0);
        String programme = programmes.isEmpty() ? "a relevant university programme" : programmes.get(0);
        String university = universities.isEmpty() ? "a suitable university" : universities.get(0).name();
        return List.of(
                "Strengthen your Grade 12 subjects aligned to the field (e.g., Maths, Physical Sciences, English).",
                "Choose a target programme: " + programme + ".",
                "Apply to universities that offer the programme, such as " + university + ".",
                "Build practical skills through projects, internships, and short courses.",
                "Transition from qualification to entry-level roles in " + career + "."
        );
    }

    private String buildAnswerText(StudentProfile profile,
                                   String message,
                                   List<String> careers,
                                   List<String> programmes,
                                   List<BookwormChatResponse.RecommendedUniversity> universities) {
        String firstCareer = careers.isEmpty() ? "career options" : careers.get(0);
        String firstProgramme = programmes.isEmpty() ? "relevant programmes" : programmes.get(0);
        String firstUniversity = universities.isEmpty() ? "universities in our database" : universities.get(0).name();
        return "Bookworm analysed your request: \"" + message + "\" and used your profile data (qualification, interests, skills, and location) "
                + "to recommend a path. Start by targeting " + firstProgramme + ", explore universities like " + firstUniversity
                + ", and align your skills toward " + firstCareer + ".";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
