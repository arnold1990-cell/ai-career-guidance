package com.edurite.student.service;

import com.edurite.application.repository.ApplicationRepository;
import com.edurite.notification.repository.NotificationRepository;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.dto.StudentProfileDto;
import com.edurite.student.dto.StudentProfileUpsertRequest;
import com.edurite.student.entity.SavedBursary;
import com.edurite.student.entity.SavedCareer;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.SavedBursaryRepository;
import com.edurite.student.repository.SavedCareerRepository;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.subscription.repository.SubscriptionRepository;
import com.edurite.upload.service.StorageService;
import com.edurite.user.entity.User;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StudentService {

    private final StudentProfileRepository repository;
    private final CurrentUserService currentUserService;
    private final StorageService storageService;
    private final SavedCareerRepository savedCareerRepository;
    private final SavedBursaryRepository savedBursaryRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationRepository notificationRepository;
    private final SubscriptionRepository subscriptionRepository;

    public StudentService(StudentProfileRepository repository, CurrentUserService currentUserService, StorageService storageService, SavedCareerRepository savedCareerRepository, SavedBursaryRepository savedBursaryRepository, ApplicationRepository applicationRepository, NotificationRepository notificationRepository, SubscriptionRepository subscriptionRepository) {
        this.repository = repository;
        this.currentUserService = currentUserService;
        this.storageService = storageService;
        this.savedCareerRepository = savedCareerRepository;
        this.savedBursaryRepository = savedBursaryRepository;
        this.applicationRepository = applicationRepository;
        this.notificationRepository = notificationRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    public StudentProfileDto getProfile(Principal principal) {
        User user = currentUserService.requireUser(principal);
        StudentProfile profile = repository.findByUserId(user.getId()).orElseGet(() -> createDefault(user));
        return toDto(profile, user.getEmail());
    }

    public StudentProfileDto upsertProfile(Principal principal, StudentProfileUpsertRequest request) {
        User user = currentUserService.requireUser(principal);
        StudentProfile profile = repository.findByUserId(user.getId()).orElseGet(() -> createDefault(user));
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setPhone(request.phone());
        profile.setDateOfBirth(request.dateOfBirth());
        profile.setGender(request.gender());
        profile.setLocation(request.location());
        profile.setBio(request.bio());
        profile.setQualificationLevel(request.qualificationLevel());
        profile.setQualifications(join(request.qualifications()));
        profile.setExperience(join(request.experience()));
        profile.setSkills(join(request.skills()));
        profile.setInterests(join(request.interests()));
        profile.setCareerGoals(request.careerGoals());
        profile.setProfileCompleted(calculateCompleteness(profile) >= 70);
        repository.save(profile);
        return toDto(profile, user.getEmail());
    }

    public StudentProfileDto uploadDocument(Principal principal, MultipartFile file, String documentType) throws IOException {
        validateFile(file);
        User user = currentUserService.requireUser(principal);
        StudentProfile profile = repository.findByUserId(user.getId()).orElseGet(() -> createDefault(user));
        String path = storageService.putObject("student-documents", "%s/%s-%s".formatted(user.getId(), documentType, file.getOriginalFilename()), file.getBytes());
        if ("cv".equalsIgnoreCase(documentType)) {
            profile.setCvFileUrl(path);
        }
        if ("transcript".equalsIgnoreCase(documentType)) {
            profile.setTranscriptFileUrl(path);
        }
        profile.setProfileCompleted(calculateCompleteness(profile) >= 70);
        repository.save(profile);
        return toDto(profile, user.getEmail());
    }

    public Map<String, Object> dashboard(Principal principal) {
        User user = currentUserService.requireUser(principal);
        StudentProfile profile = repository.findByUserId(user.getId()).orElseGet(() -> createDefault(user));
        var subscription = subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId());
        return Map.of(
                "profileCompleteness", calculateCompleteness(profile),
                "savedOpportunities", savedCareerRepository.countByStudentId(profile.getId()) + savedBursaryRepository.countByStudentId(profile.getId()),
                "activeApplications", applicationRepository.countByStudentId(profile.getId()),
                "notifications", notificationRepository.countByUserIdAndReadFalse(user.getId()),
                "subscriptionTier", subscription.map(s -> s.getPlanCode().replace("PLAN_", "")).orElse("BASIC")
        );
    }

    public void saveCareer(Principal principal, UUID careerId) {
        StudentProfile profile = getProfileEntity(principal);
        if (!savedCareerRepository.existsByStudentIdAndCareerId(profile.getId(), careerId)) {
            SavedCareer saved = new SavedCareer();
            saved.setStudentId(profile.getId());
            saved.setCareerId(careerId);
            savedCareerRepository.save(saved);
        }
    }

    public void saveBursary(Principal principal, UUID bursaryId) {
        StudentProfile profile = getProfileEntity(principal);
        if (!savedBursaryRepository.existsByStudentIdAndBursaryId(profile.getId(), bursaryId)) {
            SavedBursary saved = new SavedBursary();
            saved.setStudentId(profile.getId());
            saved.setBursaryId(bursaryId);
            savedBursaryRepository.save(saved);
        }
    }

    public StudentProfile getProfileEntity(Principal principal) {
        User user = currentUserService.requireUser(principal);
        return repository.findByUserId(user.getId()).orElseGet(() -> createDefault(user));
    }

    private StudentProfile createDefault(User user) {
        StudentProfile profile = new StudentProfile();
        profile.setUserId(user.getId());
        profile.setFirstName(user.getFirstName());
        profile.setLastName(user.getLastName());
        profile.setProfileCompleted(false);
        return repository.save(profile);
    }

    private StudentProfileDto toDto(StudentProfile profile, String email) {
        return new StudentProfileDto(
                profile.getId(), profile.getFirstName(), profile.getLastName(), email, profile.getPhone(), profile.getDateOfBirth(), profile.getGender(),
                profile.getLocation(), profile.getBio(), profile.getQualificationLevel(), split(profile.getQualifications()), split(profile.getExperience()),
                split(profile.getSkills()), split(profile.getInterests()), profile.getCareerGoals(), profile.getCvFileUrl(), profile.getTranscriptFileUrl(),
                profile.isProfileCompleted(), calculateCompleteness(profile)
        );
    }

    private List<String> split(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    private String join(List<String> values) {
        if (values == null) return null;
        return String.join(",", values);
    }

    private int calculateCompleteness(StudentProfile p) {
        int score = 0;
        if (p.getFirstName() != null) score += 10;
        if (p.getLastName() != null) score += 10;
        if (p.getPhone() != null) score += 10;
        if (p.getDateOfBirth() != null && p.getDateOfBirth().isBefore(LocalDate.now())) score += 10;
        if (p.getQualificationLevel() != null) score += 10;
        if (p.getSkills() != null) score += 15;
        if (p.getInterests() != null) score += 10;
        if (p.getCvFileUrl() != null) score += 15;
        if (p.getTranscriptFileUrl() != null) score += 10;
        return score;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File is required");
        if (file.getSize() > (5 * 1024 * 1024)) throw new IllegalArgumentException("File must be under 5MB");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(StandardCharsets.UTF_8);
        if (!(name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx"))) {
            throw new IllegalArgumentException("Supported file types: pdf, doc, docx");
        }
    }
}
