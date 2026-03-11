package com.edurite.student.service; // declares the package path for this Java file
import com.edurite.application.repository.ApplicationRepository; // imports a class so it can be used in this file
import com.edurite.notification.repository.NotificationRepository; // imports a class so it can be used in this file
import com.edurite.security.service.CurrentUserService; // imports a class so it can be used in this file
import com.edurite.student.dto.StudentProfileDto; // imports a class so it can be used in this file
import com.edurite.student.dto.StudentProfileUpsertRequest; // imports a class so it can be used in this file
import com.edurite.student.dto.StudentSettingsDto; // imports a class so it can be used in this file
import com.edurite.student.entity.SavedBursary; // imports a class so it can be used in this file
import com.edurite.student.entity.SavedCareer; // imports a class so it can be used in this file
import com.edurite.student.entity.StudentProfile; // imports a class so it can be used in this file
import com.edurite.student.repository.SavedBursaryRepository; // imports a class so it can be used in this file
import com.edurite.student.repository.SavedCareerRepository; // imports a class so it can be used in this file
import com.edurite.student.repository.StudentProfileRepository; // imports a class so it can be used in this file
import com.edurite.subscription.repository.SubscriptionRepository; // imports a class so it can be used in this file
import com.edurite.upload.service.StorageService; // imports a class so it can be used in this file
import com.edurite.user.entity.User; // imports a class so it can be used in this file
import java.io.IOException; // imports a class so it can be used in this file
import java.util.Locale; // imports a class so it can be used in this file
import java.security.Principal; // imports a class so it can be used in this file
import java.time.LocalDate; // imports a class so it can be used in this file
import java.util.Arrays; // imports a class so it can be used in this file
import java.util.List; // imports a class so it can be used in this file
import java.util.LinkedHashMap; // imports a class so it can be used in this file
import java.util.Map; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import org.springframework.stereotype.Service; // imports a class so it can be used in this file
import org.springframework.web.multipart.MultipartFile; // imports a class so it can be used in this file

// @Service marks a class that contains business logic.
@Service // marks this class as a service containing business logic
/**
 * This class named StudentService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class StudentService { // defines a class type

    private final StudentProfileRepository repository; // reads or writes data through the database layer
    private final CurrentUserService currentUserService; // executes this statement as part of the application logic
    private final StorageService storageService; // executes this statement as part of the application logic
    private final SavedCareerRepository savedCareerRepository; // reads or writes data through the database layer
    private final SavedBursaryRepository savedBursaryRepository; // reads or writes data through the database layer
    private final ApplicationRepository applicationRepository; // reads or writes data through the database layer
    private final NotificationRepository notificationRepository; // reads or writes data through the database layer
    private final SubscriptionRepository subscriptionRepository; // reads or writes data through the database layer

    public StudentService(StudentProfileRepository repository, CurrentUserService currentUserService, StorageService storageService, SavedCareerRepository savedCareerRepository, SavedBursaryRepository savedBursaryRepository, ApplicationRepository applicationRepository, NotificationRepository notificationRepository, SubscriptionRepository subscriptionRepository) { // reads or writes data through the database layer
        this.repository = repository; // reads or writes data through the database layer
        this.currentUserService = currentUserService; // executes this statement as part of the application logic
        this.storageService = storageService; // executes this statement as part of the application logic
        this.savedCareerRepository = savedCareerRepository; // reads or writes data through the database layer
        this.savedBursaryRepository = savedBursaryRepository; // reads or writes data through the database layer
        this.applicationRepository = applicationRepository; // reads or writes data through the database layer
        this.notificationRepository = notificationRepository; // reads or writes data through the database layer
        this.subscriptionRepository = subscriptionRepository; // reads or writes data through the database layer
    } // ends the current code block

    /**
     * Note: this method handles the "getProfile" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentProfileDto getProfile(Principal principal) { // declares a method that defines behavior for this class
        User user = currentUserService.requireUser(principal); // executes this statement as part of the application logic
        StudentProfile profile = repository.findByUserId(user.getId()).orElseGet(() -> createDefault(user)); // reads or writes data through the database layer
        return toDto(profile, user.getEmail()); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "upsertProfile" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentProfileDto upsertProfile(Principal principal, StudentProfileUpsertRequest request) { // declares a method that defines behavior for this class
        User user = currentUserService.requireUser(principal); // executes this statement as part of the application logic
        StudentProfile profile = repository.findByUserId(user.getId()).orElseGet(() -> createDefault(user)); // reads or writes data through the database layer
        profile.setFirstName(request.firstName()); // executes this statement as part of the application logic
        profile.setLastName(request.lastName()); // executes this statement as part of the application logic
        profile.setPhone(request.phone()); // executes this statement as part of the application logic
        profile.setDateOfBirth(request.dateOfBirth()); // executes this statement as part of the application logic
        profile.setGender(request.gender()); // executes this statement as part of the application logic
        profile.setLocation(request.location()); // executes this statement as part of the application logic
        profile.setBio(request.bio()); // executes this statement as part of the application logic
        profile.setQualificationLevel(request.qualificationLevel()); // executes this statement as part of the application logic
        profile.setQualifications(join(request.qualifications())); // executes this statement as part of the application logic
        profile.setExperience(join(request.experience())); // executes this statement as part of the application logic
        profile.setSkills(join(request.skills())); // executes this statement as part of the application logic
        profile.setInterests(join(request.interests())); // executes this statement as part of the application logic
        profile.setCareerGoals(request.careerGoals()); // executes this statement as part of the application logic
        profile.setProfileCompleted(calculateCompleteness(profile) >= 70); // executes this statement as part of the application logic
        repository.save(profile); // reads or writes data through the database layer
        return toDto(profile, user.getEmail()); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "uploadDocument" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentProfileDto uploadDocument(Principal principal, MultipartFile file, String documentType) throws IOException { // declares a method that defines behavior for this class
        validateFile(file); // executes this statement as part of the application logic
        User user = currentUserService.requireUser(principal); // executes this statement as part of the application logic
        StudentProfile profile = repository.findByUserId(user.getId()).orElseGet(() -> createDefault(user)); // reads or writes data through the database layer
        String path = storageService.putObject("student-documents", "%s/%s-%s".formatted(user.getId(), documentType, file.getOriginalFilename()), file.getBytes()); // executes this statement as part of the application logic
        if ("cv".equalsIgnoreCase(documentType)) { // checks a condition and runs this block only when true
            profile.setCvFileUrl(path); // executes this statement as part of the application logic
        } // ends the current code block
        if ("transcript".equalsIgnoreCase(documentType)) { // checks a condition and runs this block only when true
            profile.setTranscriptFileUrl(path); // executes this statement as part of the application logic
        } // ends the current code block
        profile.setProfileCompleted(calculateCompleteness(profile) >= 70); // executes this statement as part of the application logic
        repository.save(profile); // reads or writes data through the database layer
        return toDto(profile, user.getEmail()); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "dashboard" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public Map<String, Object> dashboard(Principal principal) { // declares a method that defines behavior for this class
        User user = currentUserService.requireUser(principal); // executes this statement as part of the application logic
        StudentProfile profile = repository.findByUserId(user.getId()).orElseGet(() -> createDefault(user)); // reads or writes data through the database layer
        var subscription = subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId()); // reads or writes data through the database layer
        long savedCareers = savedCareerRepository.countByStudentId(profile.getId()); // reads or writes data through the database layer
        long savedBursaries = savedBursaryRepository.countByStudentId(profile.getId()); // reads or writes data through the database layer
        long activeApplications = applicationRepository.countByStudentId(profile.getId()); // reads or writes data through the database layer
        List<String> skillGaps = List.of("Advanced communication", "Data analysis"); // executes this statement as part of the application logic
        List<String> improvements = List.of("Upload latest transcript", "Add one internship experience item"); // executes this statement as part of the application logic
        Map<String, Object> response = new LinkedHashMap<>(); // creates a new object instance and stores it in a variable
        response.put("profileCompleteness", calculateCompleteness(profile)); // executes this statement as part of the application logic
        response.put("savedCareers", savedCareers); // executes this statement as part of the application logic
        response.put("savedBursaries", savedBursaries); // executes this statement as part of the application logic
        response.put("savedOpportunities", savedCareers + savedBursaries); // executes this statement as part of the application logic
        response.put("activeApplications", activeApplications); // executes this statement as part of the application logic
        response.put("applicationProgress", List.of( // supports the surrounding application logic
                Map.of("label", "Draft", "count", applicationRepository.countByStudentIdAndStatus(profile.getId(), "DRAFT")), // reads or writes data through the database layer
                Map.of("label", "Submitted", "count", applicationRepository.countByStudentIdAndStatus(profile.getId(), "SUBMITTED")), // reads or writes data through the database layer
                Map.of("label", "In review", "count", applicationRepository.countByStudentIdAndStatus(profile.getId(), "IN_REVIEW")), // reads or writes data through the database layer
                Map.of("label", "Shortlisted", "count", applicationRepository.countByStudentIdAndStatus(profile.getId(), "SHORTLISTED")) // reads or writes data through the database layer
        )); // executes this statement as part of the application logic
        response.put("skillGaps", skillGaps); // executes this statement as part of the application logic
        response.put("recommendedImprovements", improvements); // executes this statement as part of the application logic
        response.put("notifications", notificationRepository.countByUserIdAndReadFalse(user.getId())); // reads or writes data through the database layer
        response.put("subscriptionTier", subscription.map(s -> { // supports the surrounding application logic
            String planCode = s.getPlanCode(); // executes this statement as part of the application logic
            return planCode == null || planCode.isBlank() ? "BASIC" : planCode.replace("PLAN_", ""); // returns a value from this method to the caller
        }).orElse("BASIC")); // executes this statement as part of the application logic
        return response; // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "getSettings" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentSettingsDto getSettings(Principal principal) { // declares a method that defines behavior for this class
        StudentProfile profile = getProfileEntity(principal); // executes this statement as part of the application logic
        return toSettingsDto(profile); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "updateSettings" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentSettingsDto updateSettings(Principal principal, StudentSettingsDto request) { // declares a method that defines behavior for this class
        StudentProfile profile = getProfileEntity(principal); // executes this statement as part of the application logic
        profile.setInAppNotificationsEnabled(request.inAppNotificationsEnabled()); // executes this statement as part of the application logic
        profile.setEmailNotificationsEnabled(request.emailNotificationsEnabled()); // executes this statement as part of the application logic
        profile.setSmsNotificationsEnabled(request.smsNotificationsEnabled()); // executes this statement as part of the application logic
        repository.save(profile); // reads or writes data through the database layer
        return toSettingsDto(profile); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "saveCareer" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public void saveCareer(Principal principal, UUID careerId) { // declares a method that defines behavior for this class
        StudentProfile profile = getProfileEntity(principal); // executes this statement as part of the application logic
        if (!savedCareerRepository.existsByStudentIdAndCareerId(profile.getId(), careerId)) { // checks a condition and runs this block only when true
            SavedCareer saved = new SavedCareer(); // creates a new object instance and stores it in a variable
            saved.setStudentId(profile.getId()); // executes this statement as part of the application logic
            saved.setCareerId(careerId); // executes this statement as part of the application logic
            savedCareerRepository.save(saved); // reads or writes data through the database layer
        } // ends the current code block
    } // ends the current code block

    /**
     * Note: this method handles the "saveBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public void saveBursary(Principal principal, UUID bursaryId) { // declares a method that defines behavior for this class
        StudentProfile profile = getProfileEntity(principal); // executes this statement as part of the application logic
        if (!savedBursaryRepository.existsByStudentIdAndBursaryId(profile.getId(), bursaryId)) { // checks a condition and runs this block only when true
            SavedBursary saved = new SavedBursary(); // creates a new object instance and stores it in a variable
            saved.setStudentId(profile.getId()); // executes this statement as part of the application logic
            saved.setBursaryId(bursaryId); // executes this statement as part of the application logic
            savedBursaryRepository.save(saved); // reads or writes data through the database layer
        } // ends the current code block
    } // ends the current code block


    /**
     * Note: this method handles the "unsaveCareer" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public void unsaveCareer(Principal principal, UUID careerId) { // declares a method that defines behavior for this class
        StudentProfile profile = getProfileEntity(principal); // executes this statement as part of the application logic
        savedCareerRepository.deleteByStudentIdAndCareerId(profile.getId(), careerId); // reads or writes data through the database layer
    } // ends the current code block

    /**
     * Note: this method handles the "unsaveBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public void unsaveBursary(Principal principal, UUID bursaryId) { // declares a method that defines behavior for this class
        StudentProfile profile = getProfileEntity(principal); // executes this statement as part of the application logic
        savedBursaryRepository.deleteByStudentIdAndBursaryId(profile.getId(), bursaryId); // reads or writes data through the database layer
    } // ends the current code block

    /**
     * Note: this method handles the "savedCareerIds" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<UUID> savedCareerIds(Principal principal) { // declares a method that defines behavior for this class
        StudentProfile profile = getProfileEntity(principal); // executes this statement as part of the application logic
        return savedCareerRepository.findByStudentId(profile.getId()).stream().map(SavedCareer::getCareerId).toList(); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "savedBursaryIds" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<UUID> savedBursaryIds(Principal principal) { // declares a method that defines behavior for this class
        StudentProfile profile = getProfileEntity(principal); // executes this statement as part of the application logic
        return savedBursaryRepository.findByStudentId(profile.getId()).stream().map(SavedBursary::getBursaryId).toList(); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "getProfileEntity" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentProfile getProfileEntity(Principal principal) { // declares a method that defines behavior for this class
        User user = currentUserService.requireUser(principal); // executes this statement as part of the application logic
        return repository.findByUserId(user.getId()).orElseGet(() -> createDefault(user)); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "createDefault" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private StudentProfile createDefault(User user) { // declares a method that defines behavior for this class
        StudentProfile profile = new StudentProfile(); // creates a new object instance and stores it in a variable
        profile.setUserId(user.getId()); // executes this statement as part of the application logic
        profile.setFirstName(user.getFirstName()); // executes this statement as part of the application logic
        profile.setLastName(user.getLastName()); // executes this statement as part of the application logic
        profile.setProfileCompleted(false); // executes this statement as part of the application logic
        profile.setInAppNotificationsEnabled(true); // executes this statement as part of the application logic
        profile.setEmailNotificationsEnabled(false); // executes this statement as part of the application logic
        profile.setSmsNotificationsEnabled(false); // executes this statement as part of the application logic
        return repository.save(profile); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "toDto" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private StudentProfileDto toDto(StudentProfile profile, String email) { // declares a method that defines behavior for this class
        return new StudentProfileDto( // returns a value from this method to the caller
                profile.getId(), profile.getFirstName(), profile.getLastName(), email, profile.getPhone(), profile.getDateOfBirth(), profile.getGender(), // supports the surrounding application logic
                profile.getLocation(), profile.getBio(), profile.getQualificationLevel(), split(profile.getQualifications()), split(profile.getExperience()), // supports the surrounding application logic
                split(profile.getSkills()), split(profile.getInterests()), profile.getCareerGoals(), profile.getCvFileUrl(), profile.getTranscriptFileUrl(), // supports the surrounding application logic
                profile.isProfileCompleted(), calculateCompleteness(profile) // supports the surrounding application logic
        ); // executes this statement as part of the application logic
    } // ends the current code block

    /**
     * Note: this method handles the "toSettingsDto" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private StudentSettingsDto toSettingsDto(StudentProfile profile) { // declares a method that defines behavior for this class
        return new StudentSettingsDto(profile.isInAppNotificationsEnabled(), profile.isEmailNotificationsEnabled(), profile.isSmsNotificationsEnabled()); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "split" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private List<String> split(String value) { // declares a method that defines behavior for this class
        if (value == null || value.isBlank()) return List.of(); // checks a condition and runs this block only when true
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList(); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "join" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private String join(List<String> values) { // declares a method that defines behavior for this class
        if (values == null) return null; // checks a condition and runs this block only when true
        return String.join(",", values); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "calculateCompleteness" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private int calculateCompleteness(StudentProfile p) { // declares a method that defines behavior for this class
        int score = 0; // executes this statement as part of the application logic
        if (p.getFirstName() != null) score += 10; // checks a condition and runs this block only when true
        if (p.getLastName() != null) score += 10; // checks a condition and runs this block only when true
        if (p.getPhone() != null) score += 10; // checks a condition and runs this block only when true
        if (p.getDateOfBirth() != null && p.getDateOfBirth().isBefore(LocalDate.now())) score += 10; // checks a condition and runs this block only when true
        if (p.getQualificationLevel() != null) score += 10; // checks a condition and runs this block only when true
        if (p.getSkills() != null) score += 15; // checks a condition and runs this block only when true
        if (p.getInterests() != null) score += 10; // checks a condition and runs this block only when true
        if (p.getCvFileUrl() != null) score += 15; // checks a condition and runs this block only when true
        if (p.getTranscriptFileUrl() != null) score += 10; // checks a condition and runs this block only when true
        return score; // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "validateFile" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private void validateFile(MultipartFile file) { // declares a method that defines behavior for this class
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File is required"); // checks a condition and runs this block only when true
        if (file.getSize() > (5 * 1024 * 1024)) throw new IllegalArgumentException("File must be under 5MB"); // checks a condition and runs this block only when true
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT); // executes this statement as part of the application logic
        if (!(name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx"))) { // checks a condition and runs this block only when true
            throw new IllegalArgumentException("Supported file types: pdf, doc, docx"); // throws an exception to signal an error condition
        } // ends the current code block
    } // ends the current code block
} // ends the current code block
