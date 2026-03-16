package com.edurite.ai.bookworm;

import com.edurite.ai.bookworm.dto.BookwormChatRequest;
import com.edurite.ai.bookworm.dto.BookwormChatResponse;
import com.edurite.ai.context.AiGuidanceContextService;
import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.service.GeminiService;
import com.edurite.ai.service.UniversitySourcesGuidanceService;
import com.edurite.bursary.repository.BursaryRepository;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.student.service.StudentService;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BookwormService {

    private final StudentService studentService;
    private final StudentProfileRepository studentProfileRepository;
    private final UniversitySourcesGuidanceService universitySourcesGuidanceService;
    private final AiGuidanceContextService contextService;
    private final BursaryRepository bursaryRepository;
    private final GeminiService geminiService;

    public BookwormService(
            StudentService studentService,
            StudentProfileRepository studentProfileRepository,
            UniversitySourcesGuidanceService universitySourcesGuidanceService,
            AiGuidanceContextService contextService,
            BursaryRepository bursaryRepository,
            GeminiService geminiService
    ) {
        this.studentService = studentService;
        this.studentProfileRepository = studentProfileRepository;
        this.universitySourcesGuidanceService = universitySourcesGuidanceService;
        this.contextService = contextService;
        this.bursaryRepository = bursaryRepository;
        this.geminiService = geminiService;
    }

    public BookwormChatResponse chat(Principal principal, BookwormChatRequest request) {
        StudentProfile profile = resolveProfile(principal, request.studentProfileId());
        UniversitySourcesAnalysisResponse guidance = universitySourcesGuidanceService.analyse(principal,
                new UniversitySourcesAnalysisRequest(List.of(), request.question(), request.question(), profile.getQualificationLevel(), 6));

        AiGuidanceContextService.AiGuidanceContext context = contextService.build(profile);
        List<String> bursaryLinks = bursaryRepository.findAll().stream()
                .filter(b -> b.getTitle() != null)
                .filter(b -> matchesQuestion(b.getTitle(), request.question()) || matchesQuestion(b.getFieldOfStudy(), request.question()))
                .limit(3)
                .map(b -> "Bursary: " + b.getTitle())
                .toList();

        List<BookwormChatResponse.UniversityLink> universities = context.universities().stream()
                .filter(university -> guidance.recommendedUniversities().stream()
                        .anyMatch(rec -> rec.toLowerCase(Locale.ROOT).contains(university.name().toLowerCase(Locale.ROOT))
                                || university.name().toLowerCase(Locale.ROOT).contains(rec.toLowerCase(Locale.ROOT))))
                .limit(5)
                .map(university -> new BookwormChatResponse.UniversityLink(university.name(), university.website()))
                .toList();

        List<String> combinedLinks = new ArrayList<>();
        combinedLinks.addAll(universities.stream().map(u -> u.name() + " - " + (u.website() == null ? "Website unavailable" : u.website())).toList());
        combinedLinks.addAll(bursaryLinks);

        String answer = geminiService.buildBookwormAnswer(request.question(), guidance.summary(), guidance.recommendedNextSteps(), combinedLinks);

        return new BookwormChatResponse(
                answer,
                guidance.recommendedCareers().stream().map(UniversitySourcesAnalysisResponse.RecommendedCareer::name).toList(),
                guidance.recommendedProgrammes().stream().map(UniversitySourcesAnalysisResponse.RecommendedProgramme::name).toList(),
                universities,
                combinedLinks
        );
    }

    private StudentProfile resolveProfile(Principal principal, UUID studentProfileId) {
        StudentProfile principalProfile = studentService.getProfileEntity(principal);
        if (studentProfileId == null || principalProfile.getId().equals(studentProfileId)) {
            return principalProfile;
        }
        return studentProfileRepository.findById(studentProfileId)
                .filter(profile -> profile.getUserId().equals(principalProfile.getUserId()))
                .orElse(principalProfile);
    }

    private boolean matchesQuestion(String value, String question) {
        if (value == null || question == null) {
            return false;
        }
        String normalizedValue = value.toLowerCase(Locale.ROOT);
        for (String word : question.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (word.length() > 2 && normalizedValue.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
