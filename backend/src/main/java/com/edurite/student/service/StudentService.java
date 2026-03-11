package com.edurite.student.service;
import com.edurite.application.repository.ApplicationRepository;
import com.edurite.notification.repository.NotificationRepository;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.dto.StudentProfileDto;
import com.edurite.student.dto.StudentProfileUpsertRequest;
import com.edurite.student.dto.StudentSettingsDto;
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
import java.util.Locale;
import java.security.Principal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// @Service marks a class that contains business logic.
@Service
/**
 * This class named StudentService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
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

    /**
     * Beginner note: this method handles the "getProfile" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentProfileDto getProfile(Principal principal) {
        User user = currentUserService.requireUser(principal);
        StudentProfile profile = repository.findByUserId(user.getId()).orElseGet(() -> createDefault(user));
        return toDto(profile, user.getEmail());
    }

    /**
     * Beginner note: this method handles the "upsertProfile" step of the feature.
     * It exists to keep this class focused and reusable.
     */
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

    /**
     * Beginner note: this method handles the "uploadDocument" step of the feature.
     * It exists to keep this class focused and reusable.
     */
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

    /**
     * Beginner note: this method handles the "dashboard" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public Map<String, Object> dashboard(Principal principal) {
        User user = currentUserService.requireUser(principal);
        StudentProfile profile = repository.findByUserId(user.getId()).orElseGet(() -> createDefault(user));
        var subscription = subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId());
        long savedCareers = savedCareerRepository.countByStudentId(profile.getId());
        long savedBursaries = savedBursaryRepository.countByStudentId(profile.getId());
        long activeApplications = applicationRepository.countByStudentId(profile.getId());
        List<String> skillGaps = List.of("Advanced communication", "Data analysis");
        List<String> improvements = List.of("Upload latest transcript", "Add one internship experience item");
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("profileCompleteness", calculateCompleteness(profile));
        response.put("savedCareers", savedCareers);
        response.put("savedBursaries", savedBursaries);
        response.put("savedOpportunities", savedCareers + savedBursaries);
        response.put("activeApplications", activeApplications);
        response.put("applicationProgress", List.of(
                Map.of("label", "Draft", "count", applicationRepository.countByStudentIdAndStatus(profile.getId(), "DRAFT")),
                Map.of("label", "Submitted", "count", applicationRepository.countByStudentIdAndStatus(profile.getId(), "SUBMITTED")),
                Map.of("label", "In review", "count", applicationRepository.countByStudentIdAndStatus(profile.getId(), "IN_REVIEW")),
                Map.of("label", "Shortlisted", "count", applicationRepository.countByStudentIdAndStatus(profile.getId(), "SHORTLISTED"))
        ));
        response.put("skillGaps", skillGaps);
        response.put("recommendedImprovements", improvements);
        response.put("notifications", notificationRepository.countByUserIdAndReadFalse(user.getId()));
        response.put("subscriptionTier", subscription.map(s -> {
            String planCode = s.getPlanCode();
            return planCode == null || planCode.isBlank() ? "BASIC" : planCode.replace("PLAN_", "");
        }).orElse("BASIC"));
        return response;
    }

    /**
     * Beginner note: this method handles the "getSettings" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentSettingsDto getSettings(Principal principal) {
        StudentProfile profile = getProfileEntity(principal);
        return toSettingsDto(profile);
    }

    /**
     * Beginner note: this method handles the "updateSettings" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentSettingsDto updateSettings(Principal principal, StudentSettingsDto request) {
        StudentProfile profile = getProfileEntity(principal);
        profile.setInAppNotificationsEnabled(request.inAppNotificationsEnabled());
        profile.setEmailNotificationsEnabled(request.emailNotificationsEnabled());
        profile.setSmsNotificationsEnabled(request.smsNotificationsEnabled());
        repository.save(profile);
        return toSettingsDto(profile);
    }

    /**
     * Beginner note: this method handles the "saveCareer" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public void saveCareer(Principal principal, UUID careerId) {
        StudentProfile profile = getProfileEntity(principal);
        if (!savedCareerRepository.existsByStudentIdAndCareerId(profile.getId(), careerId)) {
            SavedCareer saved = new SavedCareer();
            saved.setStudentId(profile.getId());
            saved.setCareerId(careerId);
            savedCareerRepository.save(saved);
        }
    }

    /**
     * Beginner note: this method handles the "saveBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public void saveBursary(Principal principal, UUID bursaryId) {
        StudentProfile profile = getProfileEntity(principal);
        if (!savedBursaryRepository.existsByStudentIdAndBursaryId(profile.getId(), bursaryId)) {
            SavedBursary saved = new SavedBursary();
            saved.setStudentId(profile.getId());
            saved.setBursaryId(bursaryId);
            savedBursaryRepository.save(saved);
        }
    }


    /**
     * Beginner note: this method handles the "unsaveCareer" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public void unsaveCareer(Principal principal, UUID careerId) {
        StudentProfile profile = getProfileEntity(principal);
        savedCareerRepository.deleteByStudentIdAndCareerId(profile.getId(), careerId);
    }

    /**
     * Beginner note: this method handles the "unsaveBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public void unsaveBursary(Principal principal, UUID bursaryId) {
        StudentProfile profile = getProfileEntity(principal);
        savedBursaryRepository.deleteByStudentIdAndBursaryId(profile.getId(), bursaryId);
    }

    /**
     * Beginner note: this method handles the "savedCareerIds" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<UUID> savedCareerIds(Principal principal) {
        StudentProfile profile = getProfileEntity(principal);
        return savedCareerRepository.findByStudentId(profile.getId()).stream().map(SavedCareer::getCareerId).toList();
    }

    /**
     * Beginner note: this method handles the "savedBursaryIds" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<UUID> savedBursaryIds(Principal principal) {
        StudentProfile profile = getProfileEntity(principal);
        return savedBursaryRepository.findByStudentId(profile.getId()).stream().map(SavedBursary::getBursaryId).toList();
    }

    /**
     * Beginner note: this method handles the "getProfileEntity" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentProfile getProfileEntity(Principal principal) {
        User user = currentUserService.requireUser(principal);
        return repository.findByUserId(user.getId()).orElseGet(() -> createDefault(user));
    }

    /**
     * Beginner note: this method handles the "createDefault" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private StudentProfile createDefault(User user) {
        StudentProfile profile = new StudentProfile();
        profile.setUserId(user.getId());
        profile.setFirstName(user.getFirstName());
        profile.setLastName(user.getLastName());
        profile.setProfileCompleted(false);
        profile.setInAppNotificationsEnabled(true);
        profile.setEmailNotificationsEnabled(false);
        profile.setSmsNotificationsEnabled(false);
        return repository.save(profile);
    }

    /**
     * Beginner note: this method handles the "toDto" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private StudentProfileDto toDto(StudentProfile profile, String email) {
        return new StudentProfileDto(
                profile.getId(), profile.getFirstName(), profile.getLastName(), email, profile.getPhone(), profile.getDateOfBirth(), profile.getGender(),
                profile.getLocation(), profile.getBio(), profile.getQualificationLevel(), split(profile.getQualifications()), split(profile.getExperience()),
                split(profile.getSkills()), split(profile.getInterests()), profile.getCareerGoals(), profile.getCvFileUrl(), profile.getTranscriptFileUrl(),
                profile.isProfileCompleted(), calculateCompleteness(profile)
        );
    }

    /**
     * Beginner note: this method handles the "toSettingsDto" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private StudentSettingsDto toSettingsDto(StudentProfile profile) {
        return new StudentSettingsDto(profile.isInAppNotificationsEnabled(), profile.isEmailNotificationsEnabled(), profile.isSmsNotificationsEnabled());
    }

    /**
     * Beginner note: this method handles the "split" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private List<String> split(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    /**
     * Beginner note: this method handles the "join" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private String join(List<String> values) {
        if (values == null) return null;
        return String.join(",", values);
    }

    /**
     * Beginner note: this method handles the "calculateCompleteness" step of the feature.
     * It exists to keep this class focused and reusable.
     */
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

    /**
     * Beginner note: this method handles the "validateFile" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File is required");
        if (file.getSize() > (5 * 1024 * 1024)) throw new IllegalArgumentException("File must be under 5MB");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!(name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx"))) {
            throw new IllegalArgumentException("Supported file types: pdf, doc, docx");
        }
    }
}
