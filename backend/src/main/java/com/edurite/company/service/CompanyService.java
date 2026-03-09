package com.edurite.company.service;

import com.edurite.bursary.entity.Bursary;
import com.edurite.bursary.repository.BursaryRepository;
import com.edurite.common.exception.ResourceConflictException;
import com.edurite.company.dto.AdminCompanyReviewRequest;
import com.edurite.company.dto.CompanyBursaryDto;
import com.edurite.company.dto.CompanyBursaryUpsertRequest;
import com.edurite.company.dto.CompanyDocumentDto;
import com.edurite.company.dto.CompanyForgotPasswordRequest;
import com.edurite.company.dto.CompanyProfileDto;
import com.edurite.company.dto.CompanyProfileUpdateRequest;
import com.edurite.company.dto.CompanyResetPasswordRequest;
import com.edurite.company.dto.CompanyStudentSearchResultDto;
import com.edurite.company.entity.CompanyApprovalStatus;
import com.edurite.company.entity.CompanyPasswordResetToken;
import com.edurite.company.entity.CompanyProfile;
import com.edurite.company.entity.CompanyVerificationDocument;
import com.edurite.company.mapper.CompanyProfileMapper;
import com.edurite.company.repository.CompanyPasswordResetTokenRepository;
import com.edurite.company.repository.CompanyProfileRepository;
import com.edurite.company.repository.CompanyVerificationDocumentRepository;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.upload.service.StorageService;
import com.edurite.user.entity.User;
import com.edurite.user.repository.UserRepository;
import java.io.IOException;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CompanyService {

    private final CompanyProfileRepository companyRepository;
    private final CompanyVerificationDocumentRepository documentRepository;
    private final CompanyPasswordResetTokenRepository resetTokenRepository;
    private final BursaryRepository bursaryRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final CompanyProfileMapper mapper;
    private final CurrentUserService currentUserService;
    private final StorageService storageService;
    private final PasswordEncoder passwordEncoder;

    public CompanyService(CompanyProfileRepository companyRepository, CompanyVerificationDocumentRepository documentRepository, CompanyPasswordResetTokenRepository resetTokenRepository, BursaryRepository bursaryRepository, StudentProfileRepository studentProfileRepository, UserRepository userRepository, CompanyProfileMapper mapper, CurrentUserService currentUserService, StorageService storageService, PasswordEncoder passwordEncoder) {
        this.companyRepository = companyRepository;
        this.documentRepository = documentRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.bursaryRepository = bursaryRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.currentUserService = currentUserService;
        this.storageService = storageService;
        this.passwordEncoder = passwordEncoder;
    }

    public CompanyProfileDto getMe(Principal principal) {
        return mapper.toDto(requireCompany(principal));
    }

    public CompanyProfileDto updateMe(Principal principal, CompanyProfileUpdateRequest request) {
        CompanyProfile company = requireCompany(principal);
        company.setIndustry(request.industry());
        company.setMobileNumber(request.mobileNumber());
        company.setContactPersonName(request.contactPersonName());
        company.setAddress(request.address());
        company.setWebsite(request.website());
        company.setDescription(request.description());
        return mapper.toDto(companyRepository.save(company));
    }

    public CompanyDocumentDto uploadDocument(Principal principal, MultipartFile file, String documentType) throws IOException {
        CompanyProfile company = requireCompany(principal);
        String key = storageService.putObject("company-documents", "%s/%s-%s".formatted(company.getId(), documentType, file.getOriginalFilename()), file.getBytes());
        CompanyVerificationDocument document = new CompanyVerificationDocument();
        document.setCompanyId(company.getId());
        document.setDocumentType(documentType);
        document.setObjectKey(key);
        document.setFileName(file.getOriginalFilename());
        document.setUploadedBy(company.getUserId());
        CompanyVerificationDocument saved = documentRepository.save(document);
        return new CompanyDocumentDto(saved.getId(), saved.getDocumentType(), saved.getObjectKey(), saved.getVerificationStatus(), saved.getFileName(), saved.getCreatedAt());
    }

    public List<CompanyDocumentDto> listDocuments(Principal principal) {
        CompanyProfile company = requireCompany(principal);
        return documentRepository.findByCompanyIdOrderByCreatedAtDesc(company.getId()).stream()
                .map(doc -> new CompanyDocumentDto(doc.getId(), doc.getDocumentType(), doc.getObjectKey(), doc.getVerificationStatus(), doc.getFileName(), doc.getCreatedAt()))
                .toList();
    }

    public CompanyBursaryDto createBursary(Principal principal, CompanyBursaryUpsertRequest request) {
        CompanyProfile company = requireApprovedCompany(principal);
        Bursary bursary = toBursary(new Bursary(), company.getId(), request);
        bursary.setStatus("ACTIVE");
        return toBursaryDto(bursaryRepository.save(bursary));
    }

    public CompanyBursaryDto updateBursary(Principal principal, UUID bursaryId, CompanyBursaryUpsertRequest request) {
        CompanyProfile company = requireApprovedCompany(principal);
        Bursary bursary = bursaryRepository.findById(bursaryId).orElseThrow(() -> new ResourceConflictException("Bursary not found"));
        ensureOwned(company, bursary);
        return toBursaryDto(bursaryRepository.save(toBursary(bursary, company.getId(), request)));
    }

    public List<CompanyBursaryDto> listOwnBursaries(Principal principal) {
        CompanyProfile company = requireCompany(principal);
        return bursaryRepository.findByCompanyIdOrderByCreatedAtDesc(company.getId()).stream().map(this::toBursaryDto).toList();
    }

    public CompanyBursaryDto getOwnBursary(Principal principal, UUID bursaryId) {
        CompanyProfile company = requireCompany(principal);
        Bursary bursary = bursaryRepository.findById(bursaryId).orElseThrow(() -> new ResourceConflictException("Bursary not found"));
        ensureOwned(company, bursary);
        return toBursaryDto(bursary);
    }

    public CompanyBursaryDto setBursaryStatus(Principal principal, UUID bursaryId, String status) {
        CompanyProfile company = requireApprovedCompany(principal);
        Bursary bursary = bursaryRepository.findById(bursaryId).orElseThrow(() -> new ResourceConflictException("Bursary not found"));
        ensureOwned(company, bursary);
        bursary.setStatus(status.toUpperCase(Locale.ROOT));
        return toBursaryDto(bursaryRepository.save(bursary));
    }

    public List<CompanyStudentSearchResultDto> searchStudents(Principal principal, String fieldOfInterest, String qualificationLevel, String skills, String location) {
        requireApprovedCompany(principal);
        return studentProfileRepository.findAll().stream()
                .filter(s -> matches(s.getInterests(), fieldOfInterest))
                .filter(s -> matches(s.getQualificationLevel(), qualificationLevel))
                .filter(s -> matches(s.getSkills(), skills))
                .filter(s -> matches(s.getLocation(), location))
                .map(this::toStudentView)
                .toList();
    }

    @Transactional
    public String issuePasswordResetToken(CompanyForgotPasswordRequest request) {
        CompanyProfile company = findCompanyByRecoveryInput(request);
        if (company == null) {
            return "If the account exists, a reset instruction has been generated.";
        }
        resetTokenRepository.deleteByExpiresAtBefore(OffsetDateTime.now());
        resetTokenRepository.findByCompanyIdAndUsedAtIsNull(company.getId()).forEach(resetTokenRepository::delete);
        CompanyPasswordResetToken token = new CompanyPasswordResetToken();
        token.setCompanyId(company.getId());
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(OffsetDateTime.now().plusMinutes(30));
        resetTokenRepository.save(token);
        return "Password reset token generated (stub): %s".formatted(token.getToken());
    }

    @Transactional
    public void resetPassword(CompanyResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ResourceConflictException("Password confirmation mismatch");
        }
        CompanyPasswordResetToken token = resetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new ResourceConflictException("Invalid reset token"));
        if (!token.isActive()) {
            throw new ResourceConflictException("Reset token expired or already used");
        }
        CompanyProfile company = companyRepository.findById(token.getCompanyId())
                .orElseThrow(() -> new ResourceConflictException("Company not found"));
        User user = userRepository.findById(company.getUserId()).orElseThrow(() -> new ResourceConflictException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        token.setUsedAt(OffsetDateTime.now());
        resetTokenRepository.save(token);
    }

    public List<CompanyProfileDto> listPendingCompanies() {
        return companyRepository.findByStatusOrderByCreatedAtAsc(CompanyApprovalStatus.PENDING).stream().map(mapper::toDto).toList();
    }

    public CompanyProfileDto getCompanyById(UUID companyId) {
        return mapper.toDto(companyRepository.findById(companyId).orElseThrow(() -> new ResourceConflictException("Company not found")));
    }

    @Transactional
    public CompanyProfileDto approve(UUID companyId, UUID adminUserId, AdminCompanyReviewRequest request) {
        return review(companyId, adminUserId, CompanyApprovalStatus.APPROVED, request.notes());
    }

    @Transactional
    public CompanyProfileDto reject(UUID companyId, UUID adminUserId, AdminCompanyReviewRequest request) {
        return review(companyId, adminUserId, CompanyApprovalStatus.REJECTED, request.notes());
    }

    @Transactional
    public CompanyProfileDto requestMoreInfo(UUID companyId, UUID adminUserId, AdminCompanyReviewRequest request) {
        return review(companyId, adminUserId, CompanyApprovalStatus.MORE_INFO_REQUIRED, request.notes());
    }

    private CompanyProfileDto review(UUID companyId, UUID adminUserId, CompanyApprovalStatus status, String notes) {
        CompanyProfile company = companyRepository.findById(companyId).orElseThrow(() -> new ResourceConflictException("Company not found"));
        company.setStatus(status);
        company.setReviewedBy(adminUserId);
        company.setReviewedAt(OffsetDateTime.now());
        company.setReviewNotes(notes);
        return mapper.toDto(companyRepository.save(company));
    }

    private CompanyProfile requireCompany(Principal principal) {
        User user = currentUserService.requireUser(principal);
        return companyRepository.findByUserId(user.getId()).orElseThrow(() -> new ResourceConflictException("Company profile not found"));
    }

    private CompanyProfile requireApprovedCompany(Principal principal) {
        CompanyProfile company = requireCompany(principal);
        if (company.getStatus() != CompanyApprovalStatus.APPROVED) {
            throw new ResourceConflictException("Company is awaiting admin approval");
        }
        return company;
    }

    private void ensureOwned(CompanyProfile company, Bursary bursary) {
        if (!company.getId().equals(bursary.getCompanyId())) {
            throw new ResourceConflictException("Bursary does not belong to this company");
        }
    }

    private Bursary toBursary(Bursary bursary, UUID companyId, CompanyBursaryUpsertRequest request) {
        bursary.setCompanyId(companyId);
        bursary.setTitle(request.bursaryName());
        bursary.setDescription(request.description());
        bursary.setFieldOfStudy(request.fieldOfStudy());
        bursary.setQualificationLevel(request.academicLevel());
        bursary.setApplicationStartDate(request.applicationStartDate());
        bursary.setApplicationEndDate(request.applicationEndDate());
        bursary.setFundingAmount(request.fundingAmount());
        bursary.setBenefits(request.benefits());
        bursary.setRequiredSubjects(join(request.requiredSubjects()));
        bursary.setMinimumGrade(request.minimumGrade());
        bursary.setDemographics(join(request.demographics()));
        bursary.setLocation(request.location());
        bursary.setEligibility(join(request.eligibilityFilters()));
        return bursary;
    }

    private CompanyBursaryDto toBursaryDto(Bursary b) {
        return new CompanyBursaryDto(
                b.getId(), b.getTitle(), b.getDescription(), b.getFieldOfStudy(), b.getQualificationLevel(),
                b.getApplicationStartDate(), b.getApplicationEndDate(), b.getFundingAmount(), b.getBenefits(),
                b.getRequiredSubjects(), b.getMinimumGrade(), b.getDemographics(), b.getLocation(), b.getEligibility(), b.getStatus()
        );
    }

    private CompanyStudentSearchResultDto toStudentView(StudentProfile s) {
        return new CompanyStudentSearchResultDto(s.getId(), s.getFirstName(), s.getLastName(), s.getLocation(), s.getQualificationLevel(), split(s.getSkills()), split(s.getInterests()));
    }

    private List<String> split(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(",")).map(String::trim).filter(v -> !v.isBlank()).toList();
    }

    private boolean matches(String source, String filter) {
        if (filter == null || filter.isBlank()) return true;
        if (source == null || source.isBlank()) return false;
        return source.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT));
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) return null;
        return String.join(",", values);
    }

    private CompanyProfile findCompanyByRecoveryInput(CompanyForgotPasswordRequest request) {
        if (request.email() != null && !request.email().isBlank()) {
            return companyRepository.findByOfficialEmailIgnoreCase(request.email().trim().toLowerCase(Locale.ROOT)).orElse(null);
        }
        if (request.mobileNumber() != null && !request.mobileNumber().isBlank()) {
            return companyRepository.findByMobileNumber(request.mobileNumber().trim()).orElse(null);
        }
        return null;
    }
}
