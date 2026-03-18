package com.edurite.company.service;

import com.edurite.application.repository.ApplicationRepository;
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
import com.edurite.company.entity.AdminPasswordResetToken;
import com.edurite.company.entity.CompanyApprovalStatus;
import com.edurite.company.entity.CompanyBookmark;
import com.edurite.company.entity.CompanyInvitation;
import com.edurite.company.entity.CompanyMessage;
import com.edurite.company.entity.CompanyPasswordResetToken;
import com.edurite.company.entity.CompanyProfile;
import com.edurite.company.entity.CompanyShortlist;
import com.edurite.company.entity.CompanyVerificationDocument;
import com.edurite.company.mapper.CompanyProfileMapper;
import com.edurite.company.repository.AdminPasswordResetTokenRepository;
import com.edurite.company.repository.CompanyBookmarkRepository;
import com.edurite.company.repository.CompanyInvitationRepository;
import com.edurite.company.repository.CompanyMessageRepository;
import com.edurite.company.repository.CompanyPasswordResetTokenRepository;
import com.edurite.company.repository.CompanyProfileRepository;
import com.edurite.company.repository.CompanyShortlistRepository;
import com.edurite.company.repository.CompanyVerificationDocumentRepository;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.upload.service.StorageService;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.repository.UserRepository;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CompanyService {

    private final CompanyProfileRepository companyRepository;
    private final CompanyVerificationDocumentRepository documentRepository;
    private final CompanyPasswordResetTokenRepository resetTokenRepository;
    private final AdminPasswordResetTokenRepository adminResetTokenRepository;
    private final CompanyBookmarkRepository bookmarkRepository;
    private final CompanyShortlistRepository shortlistRepository;
    private final CompanyMessageRepository messageRepository;
    private final CompanyInvitationRepository invitationRepository;
    private final BursaryRepository bursaryRepository;
    private final ApplicationRepository applicationRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final CompanyProfileMapper mapper;
    private final CurrentUserService currentUserService;
    private final StorageService storageService;
    private final PasswordEncoder passwordEncoder;

    public CompanyService(
            CompanyProfileRepository companyRepository,
            CompanyVerificationDocumentRepository documentRepository,
            CompanyPasswordResetTokenRepository resetTokenRepository,
            AdminPasswordResetTokenRepository adminResetTokenRepository,
            CompanyBookmarkRepository bookmarkRepository,
            CompanyShortlistRepository shortlistRepository,
            CompanyMessageRepository messageRepository,
            CompanyInvitationRepository invitationRepository,
            BursaryRepository bursaryRepository,
            ApplicationRepository applicationRepository,
            StudentProfileRepository studentProfileRepository,
            UserRepository userRepository,
            CompanyProfileMapper mapper,
            CurrentUserService currentUserService,
            StorageService storageService,
            PasswordEncoder passwordEncoder
    ) {
        this.companyRepository = companyRepository;
        this.documentRepository = documentRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.adminResetTokenRepository = adminResetTokenRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.shortlistRepository = shortlistRepository;
        this.messageRepository = messageRepository;
        this.invitationRepository = invitationRepository;
        this.bursaryRepository = bursaryRepository;
        this.applicationRepository = applicationRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.currentUserService = currentUserService;
        this.storageService = storageService;
        this.passwordEncoder = passwordEncoder;
    }

    public CompanyProfileDto getMe(Principal principal) { return mapper.toDto(requireCompany(principal)); }

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
        bursary.setStatus("PENDING_APPROVAL");
        return toBursaryDto(bursaryRepository.save(bursary));
    }

    public CompanyBursaryDto updateBursary(Principal principal, UUID bursaryId, CompanyBursaryUpsertRequest request) {
        CompanyProfile company = requireApprovedCompany(principal);
        Bursary bursary = bursaryRepository.findById(bursaryId).orElseThrow(() -> new ResourceConflictException("Bursary not found"));
        ensureOwned(company, bursary);
        bursary = toBursary(bursary, company.getId(), request);
        if (Set.of("ACTIVE", "REJECTED", "CLOSED", "ARCHIVED").contains(bursary.getStatus())) {
            bursary.setStatus("PENDING_APPROVAL");
        }
        return toBursaryDto(bursaryRepository.save(bursary));
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
        CompanyProfile company = requireApprovedCompany(principal);
        List<UUID> bookmarked = bookmarkRepository.findByCompanyIdOrderByCreatedAtDesc(company.getId()).stream().map(CompanyBookmark::getStudentId).toList();
        List<UUID> shortlisted = shortlistRepository.findByCompanyIdOrderByCreatedAtDesc(company.getId()).stream().map(CompanyShortlist::getStudentId).toList();
        return studentProfileRepository.findAll().stream()
                .filter(s -> matches(s.getInterests(), fieldOfInterest))
                .filter(s -> matches(s.getQualificationLevel(), qualificationLevel))
                .filter(s -> matches(s.getSkills(), skills))
                .filter(s -> matches(s.getLocation(), location))
                .map(s -> toStudentView(s, bookmarked.contains(s.getId()), shortlisted.contains(s.getId()), company))
                .sorted(Comparator.comparingInt(CompanyStudentSearchResultDto::matchScore).reversed())
                .toList();
    }

    @Transactional
    public String issuePasswordResetToken(CompanyForgotPasswordRequest request) {
        CompanyProfile company = findCompanyByRecoveryInput(request);
        User admin = findAdminByRecoveryInput(request);
        if (company == null && admin == null) {
            return "If the account exists, a reset instruction has been generated.";
        }
        resetTokenRepository.deleteByExpiresAtBefore(OffsetDateTime.now());
        if (company != null) {
            resetTokenRepository.findByCompanyIdAndUsedAtIsNull(company.getId()).forEach(resetTokenRepository::delete);
            CompanyPasswordResetToken token = new CompanyPasswordResetToken();
            token.setCompanyId(company.getId());
            token.setToken(UUID.randomUUID().toString());
            token.setExpiresAt(OffsetDateTime.now().plusMinutes(30));
            resetTokenRepository.save(token);
            return "Password reset token generated (stub): %s".formatted(token.getToken());
        }
        adminResetTokenRepository.deleteByExpiresAtBefore(OffsetDateTime.now());
        AdminPasswordResetToken token = new AdminPasswordResetToken();
        token.setUserId(admin.getId());
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(OffsetDateTime.now().plusMinutes(30));
        adminResetTokenRepository.save(token);
        return "Password reset token generated (stub): %s".formatted(token.getToken());
    }

    @Transactional
    public void resetPassword(CompanyResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ResourceConflictException("Password confirmation mismatch");
        }
        CompanyPasswordResetToken companyToken = resetTokenRepository.findByToken(request.token()).orElse(null);
        if (companyToken != null) {
            if (!companyToken.isActive()) {
                throw new ResourceConflictException("Reset token expired or already used");
            }
            User user = resolveResetUser(companyToken);
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
            userRepository.save(user);
            companyToken.setUsedAt(OffsetDateTime.now());
            resetTokenRepository.save(companyToken);
            return;
        }
        AdminPasswordResetToken adminToken = adminResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new ResourceConflictException("Invalid reset token"));
        if (!adminToken.isActive()) {
            throw new ResourceConflictException("Reset token expired or already used");
        }
        User user = userRepository.findById(adminToken.getUserId()).orElseThrow(() -> new ResourceConflictException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        adminToken.setUsedAt(OffsetDateTime.now());
        adminResetTokenRepository.save(adminToken);
    }

    public List<CompanyProfileDto> listPendingCompanies() {
        return companyRepository.findByStatusOrderByCreatedAtAsc(CompanyApprovalStatus.PENDING).stream().map(mapper::toDto).toList();
    }

    public CompanyProfileDto getCompanyById(UUID companyId) {
        return mapper.toDto(companyRepository.findById(companyId).orElseThrow(() -> new ResourceConflictException("Company not found")));
    }

    @Transactional
    public CompanyProfileDto approve(UUID companyId, UUID adminUserId, AdminCompanyReviewRequest request) { return review(companyId, adminUserId, CompanyApprovalStatus.APPROVED, request.notes()); }
    @Transactional
    public CompanyProfileDto reject(UUID companyId, UUID adminUserId, AdminCompanyReviewRequest request) { return review(companyId, adminUserId, CompanyApprovalStatus.REJECTED, request.notes()); }
    @Transactional
    public CompanyProfileDto requestMoreInfo(UUID companyId, UUID adminUserId, AdminCompanyReviewRequest request) { return review(companyId, adminUserId, CompanyApprovalStatus.MORE_INFO_REQUIRED, request.notes()); }

    public Map<String, Object> dashboardSummary(Principal principal) {
        CompanyProfile company = requireCompany(principal);
        List<Bursary> bursaries = bursaryRepository.findByCompanyIdOrderByCreatedAtDesc(company.getId());
        long totalApplications = bursaries.stream().mapToLong(b -> applicationRepository.countByBursaryId(b.getId())).sum();
        Map<String, Long> byStatus = bursaries.stream().collect(java.util.stream.Collectors.groupingBy(b -> normalizeDashboardStatus(b.getStatus()), java.util.stream.Collectors.counting()));
        List<Map<String, Object>> recent = bursaries.stream().limit(5).map(b -> {
            long applicants = applicationRepository.countByBursaryId(b.getId());
            long views = applicants * 4 + 10;
            long profileViews = Math.max(1, applicants * 2);
            double completionRate = applicants == 0 ? 0.0 : Math.min(100.0, 45.0 + applicants * 5.0);
            return Map.<String, Object>of(
                    "id", b.getId(),
                    "bursaryTitle", b.getTitle(),
                    "status", normalizeDashboardStatus(b.getStatus()),
                    "postingDate", b.getCreatedAt(),
                    "expiryDate", b.getApplicationEndDate(),
                    "applicantsReceived", applicants,
                    "views", views,
                    "impressions", views,
                    "applicationCompletionRate", completionRate,
                    "profileViews", profileViews
            );
        }).toList();
        return Map.of(
                "companyStatus", company.getStatus().name(),
                "approvalNotice", company.getStatus() == CompanyApprovalStatus.APPROVED ? "Company approved for full portal access." : "Company access remains restricted until admin approval is completed.",
                "totalBursaries", bursaries.size(),
                "totalApplications", totalApplications,
                "statusCounts", byStatus,
                "recentActivity", recent,
                "popularCategories", bursaries.stream().collect(java.util.stream.Collectors.groupingBy(Bursary::getFieldOfStudy, java.util.stream.Collectors.counting())),
                "successRate", bursaries.isEmpty() ? 0.0 : Math.round((byStatus.getOrDefault("ACTIVE", 0L) * 1000.0) / bursaries.size()) / 10.0
        );
    }

    @Transactional
    public Map<String, Object> addBookmark(Principal principal, UUID studentId, Map<String, String> payload) {
        CompanyProfile company = requireApprovedCompany(principal);
        studentProfileRepository.findById(studentId).orElseThrow(() -> new ResourceConflictException("Student not found"));
        CompanyBookmark bookmark = bookmarkRepository.findByCompanyIdAndStudentId(company.getId(), studentId).orElseGet(CompanyBookmark::new);
        bookmark.setCompanyId(company.getId());
        bookmark.setStudentId(studentId);
        bookmark.setNotes(payload.get("notes"));
        CompanyBookmark saved = bookmarkRepository.save(bookmark);
        return Map.of("id", saved.getId(), "studentId", saved.getStudentId(), "notes", saved.getNotes(), "createdAt", saved.getCreatedAt());
    }

    public List<Map<String, Object>> listBookmarks(Principal principal) {
        CompanyProfile company = requireApprovedCompany(principal);
        return bookmarkRepository.findByCompanyIdOrderByCreatedAtDesc(company.getId()).stream().map(bookmark -> {
            StudentProfile student = studentProfileRepository.findById(bookmark.getStudentId()).orElse(null);
            return Map.<String, Object>of(
                    "id", bookmark.getId(),
                    "studentId", bookmark.getStudentId(),
                    "studentName", student == null ? "Unknown" : (student.getFirstName() + " " + student.getLastName()).trim(),
                    "qualificationLevel", student == null ? "" : String.valueOf(student.getQualificationLevel()),
                    "location", student == null ? "" : String.valueOf(student.getLocation()),
                    "notes", bookmark.getNotes() == null ? "" : bookmark.getNotes(),
                    "createdAt", bookmark.getCreatedAt()
            );
        }).toList();
    }

    @Transactional
    public Map<String, Object> addShortlist(Principal principal, UUID studentId, Map<String, String> payload) {
        CompanyProfile company = requireApprovedCompany(principal);
        studentProfileRepository.findById(studentId).orElseThrow(() -> new ResourceConflictException("Student not found"));
        CompanyShortlist shortlist = new CompanyShortlist();
        shortlist.setCompanyId(company.getId());
        shortlist.setStudentId(studentId);
        shortlist.setDepartment(payload.get("department"));
        shortlist.setBursaryType(payload.get("bursaryType"));
        shortlist.setNotes(payload.get("notes"));
        CompanyShortlist saved = shortlistRepository.save(shortlist);
        return Map.of("id", saved.getId(), "studentId", saved.getStudentId(), "department", String.valueOf(saved.getDepartment()), "bursaryType", String.valueOf(saved.getBursaryType()));
    }

    public List<Map<String, Object>> listShortlists(Principal principal) {
        CompanyProfile company = requireApprovedCompany(principal);
        return shortlistRepository.findByCompanyIdOrderByCreatedAtDesc(company.getId()).stream().map(shortlist -> {
            StudentProfile student = studentProfileRepository.findById(shortlist.getStudentId()).orElse(null);
            return Map.<String, Object>of(
                    "id", shortlist.getId(),
                    "studentId", shortlist.getStudentId(),
                    "studentName", student == null ? "Unknown" : (student.getFirstName() + " " + student.getLastName()).trim(),
                    "department", shortlist.getDepartment() == null ? "" : shortlist.getDepartment(),
                    "bursaryType", shortlist.getBursaryType() == null ? "" : shortlist.getBursaryType(),
                    "notes", shortlist.getNotes() == null ? "" : shortlist.getNotes(),
                    "createdAt", shortlist.getCreatedAt()
            );
        }).toList();
    }

    @Transactional
    public Map<String, Object> sendMessage(Principal principal, UUID studentId, Map<String, String> payload) {
        CompanyProfile company = requireApprovedCompany(principal);
        studentProfileRepository.findById(studentId).orElseThrow(() -> new ResourceConflictException("Student not found"));
        CompanyMessage message = new CompanyMessage();
        message.setCompanyId(company.getId());
        message.setStudentId(studentId);
        message.setSubject(payload.getOrDefault("subject", "EduRite company outreach"));
        message.setMessage(payload.getOrDefault("message", ""));
        message.setSentByUserId(company.getUserId());
        CompanyMessage saved = messageRepository.save(message);
        return Map.of("id", saved.getId(), "studentId", saved.getStudentId(), "subject", saved.getSubject(), "createdAt", saved.getCreatedAt());
    }

    public List<Map<String, Object>> listMessages(Principal principal) {
        CompanyProfile company = requireApprovedCompany(principal);
        return messageRepository.findByCompanyIdOrderByCreatedAtDesc(company.getId()).stream().map(message -> Map.<String, Object>of(
                "id", message.getId(),
                "studentId", message.getStudentId(),
                "subject", message.getSubject(),
                "message", message.getMessage(),
                "createdAt", message.getCreatedAt()
        )).toList();
    }

    @Transactional
    public Map<String, Object> sendInvitation(Principal principal, UUID studentId, Map<String, String> payload) {
        CompanyProfile company = requireApprovedCompany(principal);
        studentProfileRepository.findById(studentId).orElseThrow(() -> new ResourceConflictException("Student not found"));
        CompanyInvitation invitation = new CompanyInvitation();
        invitation.setCompanyId(company.getId());
        invitation.setStudentId(studentId);
        invitation.setInvitationType(payload.getOrDefault("invitationType", "BURSARY_APPLICATION"));
        invitation.setMessage(payload.get("message"));
        invitation.setInvitationToken(UUID.randomUUID().toString());
        invitation.setExpiresAt(OffsetDateTime.now().plusDays(14));
        if (payload.get("targetBursaryId") != null && !payload.get("targetBursaryId").isBlank()) {
            invitation.setTargetBursaryId(UUID.fromString(payload.get("targetBursaryId")));
        }
        CompanyInvitation saved = invitationRepository.save(invitation);
        return Map.of("id", saved.getId(), "token", saved.getInvitationToken(), "invitationType", saved.getInvitationType(), "expiresAt", saved.getExpiresAt());
    }

    public List<Map<String, Object>> listInvitations(Principal principal) {
        CompanyProfile company = requireApprovedCompany(principal);
        return invitationRepository.findByCompanyIdOrderByCreatedAtDesc(company.getId()).stream().map(invitation -> Map.<String, Object>of(
                "id", invitation.getId(),
                "studentId", invitation.getStudentId(),
                "invitationType", invitation.getInvitationType(),
                "token", invitation.getInvitationToken(),
                "expiresAt", invitation.getExpiresAt(),
                "createdAt", invitation.getCreatedAt()
        )).toList();
    }

    @Transactional
    @Scheduled(cron = "${edurite.bursary.archive.cron:0 0 * * * *}")
    public void archiveExpiredBursaries() {
        List<Bursary> expired = bursaryRepository.findByApplicationEndDateBeforeAndStatusIn(LocalDate.now(), List.of("ACTIVE", "PENDING_APPROVAL", "CLOSED"));
        expired.forEach(bursary -> bursary.setStatus("ARCHIVED"));
        bursaryRepository.saveAll(expired);
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
        long applicants = applicationRepository.countByBursaryId(b.getId());
        long views = applicants * 4 + 10;
        double completionRate = applicants == 0 ? 0.0 : Math.min(100.0, 45.0 + applicants * 5.0);
        return new CompanyBursaryDto(
                b.getId(), b.getTitle(), b.getDescription(), b.getFieldOfStudy(), b.getQualificationLevel(),
                b.getApplicationStartDate(), b.getApplicationEndDate(), b.getFundingAmount(), b.getBenefits(),
                b.getRequiredSubjects(), b.getMinimumGrade(), b.getDemographics(), b.getLocation(), b.getEligibility(), b.getStatus(),
                applicants, views, completionRate, Math.max(1, applicants * 2)
        );
    }

    private CompanyStudentSearchResultDto toStudentView(StudentProfile s, boolean bookmarked, boolean shortlisted, CompanyProfile company) {
        int score = scoreStudentMatch(s, company);
        return new CompanyStudentSearchResultDto(s.getId(), s.getFirstName(), s.getLastName(), s.getLocation(), s.getQualificationLevel(), split(s.getSkills()), split(s.getInterests()), score, bookmarked, shortlisted);
    }

    private int scoreStudentMatch(StudentProfile student, CompanyProfile company) {
        int score = 40;
        score += matches(student.getInterests(), company.getIndustry()) ? 20 : 0;
        score += student.getSkills() == null ? 0 : Math.min(20, split(student.getSkills()).size() * 5);
        score += student.getQualificationLevel() == null ? 0 : 10;
        score += student.isProfileCompleted() ? 10 : 0;
        return score;
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

    private User findAdminByRecoveryInput(CompanyForgotPasswordRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            return null;
        }
        return userRepository.findByEmail(request.email().trim().toLowerCase(Locale.ROOT))
                .filter(user -> user.getRoles().stream().map(Role::getName).anyMatch(name -> name.equals("ROLE_ADMIN")))
                .orElse(null);
    }

    private User resolveResetUser(CompanyPasswordResetToken token) {
        return companyRepository.findById(token.getCompanyId())
                .map(company -> userRepository.findById(company.getUserId()).orElseThrow(() -> new ResourceConflictException("User not found")))
                .orElseGet(() -> userRepository.findById(token.getCompanyId()).orElseThrow(() -> new ResourceConflictException("User not found")));
    }

    private String normalizeDashboardStatus(String status) {
        String normalized = status == null ? "PENDING_APPROVAL" : status.toUpperCase(Locale.ROOT);
        if (normalized.equals("PENDING_APPROVAL")) return "Pending Approval";
        if (normalized.equals("ACTIVE")) return "Active";
        if (normalized.equals("CLOSED")) return "Closed";
        if (normalized.equals("ARCHIVED")) return "Archived";
        if (normalized.equals("REJECTED")) return "Rejected";
        return normalized.replace('_', ' ');
    }
}
