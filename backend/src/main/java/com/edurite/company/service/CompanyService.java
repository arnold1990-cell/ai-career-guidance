package com.edurite.company.service; // declares the package path for this Java file

import com.edurite.bursary.entity.Bursary; // imports a class so it can be used in this file
import com.edurite.bursary.repository.BursaryRepository; // imports a class so it can be used in this file
import com.edurite.common.exception.ResourceConflictException; // imports a class so it can be used in this file
import com.edurite.company.dto.AdminCompanyReviewRequest; // imports a class so it can be used in this file
import com.edurite.company.dto.CompanyBursaryDto; // imports a class so it can be used in this file
import com.edurite.company.dto.CompanyBursaryUpsertRequest; // imports a class so it can be used in this file
import com.edurite.company.dto.CompanyDocumentDto; // imports a class so it can be used in this file
import com.edurite.company.dto.CompanyForgotPasswordRequest; // imports a class so it can be used in this file
import com.edurite.company.dto.CompanyProfileDto; // imports a class so it can be used in this file
import com.edurite.company.dto.CompanyProfileUpdateRequest; // imports a class so it can be used in this file
import com.edurite.company.dto.CompanyResetPasswordRequest; // imports a class so it can be used in this file
import com.edurite.company.dto.CompanyStudentSearchResultDto; // imports a class so it can be used in this file
import com.edurite.company.entity.CompanyApprovalStatus; // imports a class so it can be used in this file
import com.edurite.company.entity.CompanyPasswordResetToken; // imports a class so it can be used in this file
import com.edurite.company.entity.CompanyProfile; // imports a class so it can be used in this file
import com.edurite.company.entity.CompanyVerificationDocument; // imports a class so it can be used in this file
import com.edurite.company.mapper.CompanyProfileMapper; // imports a class so it can be used in this file
import com.edurite.company.repository.CompanyPasswordResetTokenRepository; // imports a class so it can be used in this file
import com.edurite.company.repository.CompanyProfileRepository; // imports a class so it can be used in this file
import com.edurite.company.repository.CompanyVerificationDocumentRepository; // imports a class so it can be used in this file
import com.edurite.security.service.CurrentUserService; // imports a class so it can be used in this file
import com.edurite.student.entity.StudentProfile; // imports a class so it can be used in this file
import com.edurite.student.repository.StudentProfileRepository; // imports a class so it can be used in this file
import com.edurite.upload.service.StorageService; // imports a class so it can be used in this file
import com.edurite.user.entity.User; // imports a class so it can be used in this file
import com.edurite.user.repository.UserRepository; // imports a class so it can be used in this file
import java.io.IOException; // imports a class so it can be used in this file
import java.security.Principal; // imports a class so it can be used in this file
import java.time.OffsetDateTime; // imports a class so it can be used in this file
import java.util.Arrays; // imports a class so it can be used in this file
import java.util.List; // imports a class so it can be used in this file
import java.util.Locale; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import org.springframework.security.crypto.password.PasswordEncoder; // imports a class so it can be used in this file
import org.springframework.stereotype.Service; // imports a class so it can be used in this file
import org.springframework.transaction.annotation.Transactional; // imports a class so it can be used in this file
import org.springframework.web.multipart.MultipartFile; // imports a class so it can be used in this file

// @Service marks a class that contains business logic.
@Service // marks this class as a service containing business logic
/**
 * This class named CompanyService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class CompanyService { // defines a class type

    private final CompanyProfileRepository companyRepository; // reads or writes data through the database layer
    private final CompanyVerificationDocumentRepository documentRepository; // reads or writes data through the database layer
    private final CompanyPasswordResetTokenRepository resetTokenRepository; // handles authentication or authorization to protect secure access
    private final BursaryRepository bursaryRepository; // reads or writes data through the database layer
    private final StudentProfileRepository studentProfileRepository; // reads or writes data through the database layer
    private final UserRepository userRepository; // reads or writes data through the database layer
    private final CompanyProfileMapper mapper; // executes this statement as part of the application logic
    private final CurrentUserService currentUserService; // executes this statement as part of the application logic
    private final StorageService storageService; // executes this statement as part of the application logic
    private final PasswordEncoder passwordEncoder; // handles authentication or authorization to protect secure access

    public CompanyService(CompanyProfileRepository companyRepository, CompanyVerificationDocumentRepository documentRepository, CompanyPasswordResetTokenRepository resetTokenRepository, BursaryRepository bursaryRepository, StudentProfileRepository studentProfileRepository, UserRepository userRepository, CompanyProfileMapper mapper, CurrentUserService currentUserService, StorageService storageService, PasswordEncoder passwordEncoder) { // handles authentication or authorization to protect secure access
        this.companyRepository = companyRepository; // reads or writes data through the database layer
        this.documentRepository = documentRepository; // reads or writes data through the database layer
        this.resetTokenRepository = resetTokenRepository; // handles authentication or authorization to protect secure access
        this.bursaryRepository = bursaryRepository; // reads or writes data through the database layer
        this.studentProfileRepository = studentProfileRepository; // reads or writes data through the database layer
        this.userRepository = userRepository; // reads or writes data through the database layer
        this.mapper = mapper; // executes this statement as part of the application logic
        this.currentUserService = currentUserService; // executes this statement as part of the application logic
        this.storageService = storageService; // executes this statement as part of the application logic
        this.passwordEncoder = passwordEncoder; // handles authentication or authorization to protect secure access
    } // ends the current code block

    /**
     * Note: this method handles the "getMe" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyProfileDto getMe(Principal principal) { // declares a method that defines behavior for this class
        return mapper.toDto(requireCompany(principal)); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "updateMe" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyProfileDto updateMe(Principal principal, CompanyProfileUpdateRequest request) { // declares a method that defines behavior for this class
        CompanyProfile company = requireCompany(principal); // executes this statement as part of the application logic
        company.setIndustry(request.industry()); // executes this statement as part of the application logic
        company.setMobileNumber(request.mobileNumber()); // executes this statement as part of the application logic
        company.setContactPersonName(request.contactPersonName()); // executes this statement as part of the application logic
        company.setAddress(request.address()); // executes this statement as part of the application logic
        company.setWebsite(request.website()); // executes this statement as part of the application logic
        company.setDescription(request.description()); // executes this statement as part of the application logic
        return mapper.toDto(companyRepository.save(company)); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "uploadDocument" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyDocumentDto uploadDocument(Principal principal, MultipartFile file, String documentType) throws IOException { // declares a method that defines behavior for this class
        CompanyProfile company = requireCompany(principal); // executes this statement as part of the application logic
        String key = storageService.putObject("company-documents", "%s/%s-%s".formatted(company.getId(), documentType, file.getOriginalFilename()), file.getBytes()); // executes this statement as part of the application logic
        CompanyVerificationDocument document = new CompanyVerificationDocument(); // creates a new object instance and stores it in a variable
        document.setCompanyId(company.getId()); // executes this statement as part of the application logic
        document.setDocumentType(documentType); // executes this statement as part of the application logic
        document.setObjectKey(key); // executes this statement as part of the application logic
        document.setFileName(file.getOriginalFilename()); // executes this statement as part of the application logic
        document.setUploadedBy(company.getUserId()); // executes this statement as part of the application logic
        CompanyVerificationDocument saved = documentRepository.save(document); // reads or writes data through the database layer
        return new CompanyDocumentDto(saved.getId(), saved.getDocumentType(), saved.getObjectKey(), saved.getVerificationStatus(), saved.getFileName(), saved.getCreatedAt()); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "listDocuments" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<CompanyDocumentDto> listDocuments(Principal principal) { // declares a method that defines behavior for this class
        CompanyProfile company = requireCompany(principal); // executes this statement as part of the application logic
        return documentRepository.findByCompanyIdOrderByCreatedAtDesc(company.getId()).stream() // returns a value from this method to the caller
                .map(doc -> new CompanyDocumentDto(doc.getId(), doc.getDocumentType(), doc.getObjectKey(), doc.getVerificationStatus(), doc.getFileName(), doc.getCreatedAt())) // supports the surrounding application logic
                .toList(); // executes this statement as part of the application logic
    } // ends the current code block

    /**
     * Note: this method handles the "createBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyBursaryDto createBursary(Principal principal, CompanyBursaryUpsertRequest request) { // declares a method that defines behavior for this class
        CompanyProfile company = requireApprovedCompany(principal); // executes this statement as part of the application logic
        Bursary bursary = toBursary(new Bursary(), company.getId(), request); // creates a new object instance and stores it in a variable
        bursary.setStatus("ACTIVE"); // executes this statement as part of the application logic
        return toBursaryDto(bursaryRepository.save(bursary)); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "updateBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyBursaryDto updateBursary(Principal principal, UUID bursaryId, CompanyBursaryUpsertRequest request) { // declares a method that defines behavior for this class
        CompanyProfile company = requireApprovedCompany(principal); // executes this statement as part of the application logic
        Bursary bursary = bursaryRepository.findById(bursaryId).orElseThrow(() -> new ResourceConflictException("Bursary not found")); // creates a new object instance and stores it in a variable
        ensureOwned(company, bursary); // executes this statement as part of the application logic
        return toBursaryDto(bursaryRepository.save(toBursary(bursary, company.getId(), request))); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "listOwnBursaries" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<CompanyBursaryDto> listOwnBursaries(Principal principal) { // declares a method that defines behavior for this class
        CompanyProfile company = requireCompany(principal); // executes this statement as part of the application logic
        return bursaryRepository.findByCompanyIdOrderByCreatedAtDesc(company.getId()).stream().map(this::toBursaryDto).toList(); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "getOwnBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyBursaryDto getOwnBursary(Principal principal, UUID bursaryId) { // declares a method that defines behavior for this class
        CompanyProfile company = requireCompany(principal); // executes this statement as part of the application logic
        Bursary bursary = bursaryRepository.findById(bursaryId).orElseThrow(() -> new ResourceConflictException("Bursary not found")); // creates a new object instance and stores it in a variable
        ensureOwned(company, bursary); // executes this statement as part of the application logic
        return toBursaryDto(bursary); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "setBursaryStatus" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyBursaryDto setBursaryStatus(Principal principal, UUID bursaryId, String status) { // declares a method that defines behavior for this class
        CompanyProfile company = requireApprovedCompany(principal); // executes this statement as part of the application logic
        Bursary bursary = bursaryRepository.findById(bursaryId).orElseThrow(() -> new ResourceConflictException("Bursary not found")); // creates a new object instance and stores it in a variable
        ensureOwned(company, bursary); // executes this statement as part of the application logic
        bursary.setStatus(status.toUpperCase(Locale.ROOT)); // executes this statement as part of the application logic
        return toBursaryDto(bursaryRepository.save(bursary)); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "searchStudents" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<CompanyStudentSearchResultDto> searchStudents(Principal principal, String fieldOfInterest, String qualificationLevel, String skills, String location) { // declares a method that defines behavior for this class
        requireApprovedCompany(principal); // executes this statement as part of the application logic
        return studentProfileRepository.findAll().stream() // returns a value from this method to the caller
                .filter(s -> matches(s.getInterests(), fieldOfInterest)) // supports the surrounding application logic
                .filter(s -> matches(s.getQualificationLevel(), qualificationLevel)) // supports the surrounding application logic
                .filter(s -> matches(s.getSkills(), skills)) // supports the surrounding application logic
                .filter(s -> matches(s.getLocation(), location)) // supports the surrounding application logic
                .map(this::toStudentView) // supports the surrounding application logic
                .toList(); // executes this statement as part of the application logic
    } // ends the current code block

    @Transactional // wraps this method in a database transaction for safe commit or rollback
    /**
     * Note: this method handles the "issuePasswordResetToken" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public String issuePasswordResetToken(CompanyForgotPasswordRequest request) { // handles authentication or authorization to protect secure access
        CompanyProfile company = findCompanyByRecoveryInput(request); // executes this statement as part of the application logic
        if (company == null) { // checks a condition and runs this block only when true
            return "If the account exists, a reset instruction has been generated."; // returns a value from this method to the caller
        } // ends the current code block
        resetTokenRepository.deleteByExpiresAtBefore(OffsetDateTime.now()); // handles authentication or authorization to protect secure access
        resetTokenRepository.findByCompanyIdAndUsedAtIsNull(company.getId()).forEach(resetTokenRepository::delete); // handles authentication or authorization to protect secure access
        CompanyPasswordResetToken token = new CompanyPasswordResetToken(); // creates a new object instance and stores it in a variable
        token.setCompanyId(company.getId()); // handles authentication or authorization to protect secure access
        token.setToken(UUID.randomUUID().toString()); // handles authentication or authorization to protect secure access
        token.setExpiresAt(OffsetDateTime.now().plusMinutes(30)); // handles authentication or authorization to protect secure access
        resetTokenRepository.save(token); // handles authentication or authorization to protect secure access
        return "Password reset token generated (stub): %s".formatted(token.getToken()); // returns a value from this method to the caller
    } // ends the current code block

    @Transactional // wraps this method in a database transaction for safe commit or rollback
    /**
     * Note: this method handles the "resetPassword" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public void resetPassword(CompanyResetPasswordRequest request) { // declares a method that defines behavior for this class
        if (!request.newPassword().equals(request.confirmPassword())) { // checks a condition and runs this block only when true
            throw new ResourceConflictException("Password confirmation mismatch"); // throws an exception to signal an error condition
        } // ends the current code block
        CompanyPasswordResetToken token = resetTokenRepository.findByToken(request.token()) // handles authentication or authorization to protect secure access
                .orElseThrow(() -> new ResourceConflictException("Invalid reset token")); // handles authentication or authorization to protect secure access
        if (!token.isActive()) { // checks a condition and runs this block only when true
            throw new ResourceConflictException("Reset token expired or already used"); // throws an exception to signal an error condition
        } // ends the current code block
        CompanyProfile company = companyRepository.findById(token.getCompanyId()) // handles authentication or authorization to protect secure access
                .orElseThrow(() -> new ResourceConflictException("Company not found")); // executes this statement as part of the application logic
        User user = userRepository.findById(company.getUserId()).orElseThrow(() -> new ResourceConflictException("User not found")); // creates a new object instance and stores it in a variable
        user.setPasswordHash(passwordEncoder.encode(request.newPassword())); // handles authentication or authorization to protect secure access
        userRepository.save(user); // reads or writes data through the database layer
        token.setUsedAt(OffsetDateTime.now()); // handles authentication or authorization to protect secure access
        resetTokenRepository.save(token); // handles authentication or authorization to protect secure access
    } // ends the current code block

    /**
     * Note: this method handles the "listPendingCompanies" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<CompanyProfileDto> listPendingCompanies() { // declares a method that defines behavior for this class
        return companyRepository.findByStatusOrderByCreatedAtAsc(CompanyApprovalStatus.PENDING).stream().map(mapper::toDto).toList(); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "getCompanyById" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyProfileDto getCompanyById(UUID companyId) { // declares a method that defines behavior for this class
        return mapper.toDto(companyRepository.findById(companyId).orElseThrow(() -> new ResourceConflictException("Company not found"))); // returns a value from this method to the caller
    } // ends the current code block

    @Transactional // wraps this method in a database transaction for safe commit or rollback
    /**
     * Note: this method handles the "approve" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyProfileDto approve(UUID companyId, UUID adminUserId, AdminCompanyReviewRequest request) { // declares a method that defines behavior for this class
        return review(companyId, adminUserId, CompanyApprovalStatus.APPROVED, request.notes()); // returns a value from this method to the caller
    } // ends the current code block

    @Transactional // wraps this method in a database transaction for safe commit or rollback
    /**
     * Note: this method handles the "reject" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyProfileDto reject(UUID companyId, UUID adminUserId, AdminCompanyReviewRequest request) { // declares a method that defines behavior for this class
        return review(companyId, adminUserId, CompanyApprovalStatus.REJECTED, request.notes()); // returns a value from this method to the caller
    } // ends the current code block

    @Transactional // wraps this method in a database transaction for safe commit or rollback
    /**
     * Note: this method handles the "requestMoreInfo" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyProfileDto requestMoreInfo(UUID companyId, UUID adminUserId, AdminCompanyReviewRequest request) { // declares a method that defines behavior for this class
        return review(companyId, adminUserId, CompanyApprovalStatus.MORE_INFO_REQUIRED, request.notes()); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "review" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private CompanyProfileDto review(UUID companyId, UUID adminUserId, CompanyApprovalStatus status, String notes) { // declares a method that defines behavior for this class
        CompanyProfile company = companyRepository.findById(companyId).orElseThrow(() -> new ResourceConflictException("Company not found")); // creates a new object instance and stores it in a variable
        company.setStatus(status); // executes this statement as part of the application logic
        company.setReviewedBy(adminUserId); // executes this statement as part of the application logic
        company.setReviewedAt(OffsetDateTime.now()); // executes this statement as part of the application logic
        company.setReviewNotes(notes); // executes this statement as part of the application logic
        return mapper.toDto(companyRepository.save(company)); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "requireCompany" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private CompanyProfile requireCompany(Principal principal) { // declares a method that defines behavior for this class
        User user = currentUserService.requireUser(principal); // executes this statement as part of the application logic
        return companyRepository.findByUserId(user.getId()).orElseThrow(() -> new ResourceConflictException("Company profile not found")); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "requireApprovedCompany" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private CompanyProfile requireApprovedCompany(Principal principal) { // declares a method that defines behavior for this class
        CompanyProfile company = requireCompany(principal); // executes this statement as part of the application logic
        if (company.getStatus() != CompanyApprovalStatus.APPROVED) { // checks a condition and runs this block only when true
            throw new ResourceConflictException("Company is awaiting admin approval"); // throws an exception to signal an error condition
        } // ends the current code block
        return company; // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "ensureOwned" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private void ensureOwned(CompanyProfile company, Bursary bursary) { // declares a method that defines behavior for this class
        if (!company.getId().equals(bursary.getCompanyId())) { // checks a condition and runs this block only when true
            throw new ResourceConflictException("Bursary does not belong to this company"); // throws an exception to signal an error condition
        } // ends the current code block
    } // ends the current code block

    /**
     * Note: this method handles the "toBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private Bursary toBursary(Bursary bursary, UUID companyId, CompanyBursaryUpsertRequest request) { // declares a method that defines behavior for this class
        bursary.setCompanyId(companyId); // executes this statement as part of the application logic
        bursary.setTitle(request.bursaryName()); // executes this statement as part of the application logic
        bursary.setDescription(request.description()); // executes this statement as part of the application logic
        bursary.setFieldOfStudy(request.fieldOfStudy()); // executes this statement as part of the application logic
        bursary.setQualificationLevel(request.academicLevel()); // executes this statement as part of the application logic
        bursary.setApplicationStartDate(request.applicationStartDate()); // executes this statement as part of the application logic
        bursary.setApplicationEndDate(request.applicationEndDate()); // executes this statement as part of the application logic
        bursary.setFundingAmount(request.fundingAmount()); // executes this statement as part of the application logic
        bursary.setBenefits(request.benefits()); // executes this statement as part of the application logic
        bursary.setRequiredSubjects(join(request.requiredSubjects())); // executes this statement as part of the application logic
        bursary.setMinimumGrade(request.minimumGrade()); // executes this statement as part of the application logic
        bursary.setDemographics(join(request.demographics())); // executes this statement as part of the application logic
        bursary.setLocation(request.location()); // executes this statement as part of the application logic
        bursary.setEligibility(join(request.eligibilityFilters())); // executes this statement as part of the application logic
        return bursary; // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "toBursaryDto" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private CompanyBursaryDto toBursaryDto(Bursary b) { // declares a method that defines behavior for this class
        return new CompanyBursaryDto( // returns a value from this method to the caller
                b.getId(), b.getTitle(), b.getDescription(), b.getFieldOfStudy(), b.getQualificationLevel(), // supports the surrounding application logic
                b.getApplicationStartDate(), b.getApplicationEndDate(), b.getFundingAmount(), b.getBenefits(), // supports the surrounding application logic
                b.getRequiredSubjects(), b.getMinimumGrade(), b.getDemographics(), b.getLocation(), b.getEligibility(), b.getStatus() // supports the surrounding application logic
        ); // executes this statement as part of the application logic
    } // ends the current code block

    /**
     * Note: this method handles the "toStudentView" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private CompanyStudentSearchResultDto toStudentView(StudentProfile s) { // declares a method that defines behavior for this class
        return new CompanyStudentSearchResultDto(s.getId(), s.getFirstName(), s.getLastName(), s.getLocation(), s.getQualificationLevel(), split(s.getSkills()), split(s.getInterests())); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "split" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private List<String> split(String value) { // declares a method that defines behavior for this class
        if (value == null || value.isBlank()) return List.of(); // checks a condition and runs this block only when true
        return Arrays.stream(value.split(",")).map(String::trim).filter(v -> !v.isBlank()).toList(); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "matches" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private boolean matches(String source, String filter) { // declares a method that defines behavior for this class
        if (filter == null || filter.isBlank()) return true; // checks a condition and runs this block only when true
        if (source == null || source.isBlank()) return false; // checks a condition and runs this block only when true
        return source.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT)); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "join" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private String join(List<String> values) { // declares a method that defines behavior for this class
        if (values == null || values.isEmpty()) return null; // checks a condition and runs this block only when true
        return String.join(",", values); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "findCompanyByRecoveryInput" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private CompanyProfile findCompanyByRecoveryInput(CompanyForgotPasswordRequest request) { // declares a method that defines behavior for this class
        if (request.email() != null && !request.email().isBlank()) { // checks a condition and runs this block only when true
            return companyRepository.findByOfficialEmailIgnoreCase(request.email().trim().toLowerCase(Locale.ROOT)).orElse(null); // returns a value from this method to the caller
        } // ends the current code block
        if (request.mobileNumber() != null && !request.mobileNumber().isBlank()) { // checks a condition and runs this block only when true
            return companyRepository.findByMobileNumber(request.mobileNumber().trim()).orElse(null); // returns a value from this method to the caller
        } // ends the current code block
        return null; // returns a value from this method to the caller
    } // ends the current code block
} // ends the current code block
