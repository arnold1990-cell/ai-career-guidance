package com.edurite.admin.service;

import com.edurite.admin.entity.AuditLog;
import com.edurite.admin.entity.RolePermission;
import com.edurite.admin.repository.AuditLogRepository;
import com.edurite.admin.repository.RolePermissionRepository;
import com.edurite.application.repository.ApplicationRepository;
import com.edurite.bursary.entity.Bursary;
import com.edurite.bursary.repository.BursaryRepository;
import com.edurite.common.exception.ResourceConflictException;
import com.edurite.company.entity.CompanyApprovalStatus;
import com.edurite.company.repository.CompanyProfileRepository;
import com.edurite.security.service.CurrentUserService;
import com.edurite.subscription.entity.PaymentRecord;
import com.edurite.subscription.entity.SubscriptionRecord;
import com.edurite.subscription.repository.PaymentRepository;
import com.edurite.subscription.repository.SubscriptionRepository;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminService {

    private static final List<String> DEFAULT_ROLE_PERMISSIONS = List.of(
            "ADMIN_DASHBOARD_VIEW", "USER_MANAGE", "ROLE_MANAGE", "BURSARY_REVIEW", "SUBSCRIPTION_VIEW", "ANALYTICS_VIEW"
    );

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final AuditLogRepository auditLogRepository;
    private final BursaryRepository bursaryRepository;
    private final ApplicationRepository applicationRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public AdminService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RolePermissionRepository rolePermissionRepository,
            AuditLogRepository auditLogRepository,
            BursaryRepository bursaryRepository,
            ApplicationRepository applicationRepository,
            CompanyProfileRepository companyProfileRepository,
            SubscriptionRepository subscriptionRepository,
            PaymentRepository paymentRepository,
            CurrentUserService currentUserService,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.auditLogRepository = auditLogRepository;
        this.bursaryRepository = bursaryRepository;
        this.applicationRepository = applicationRepository;
        this.companyProfileRepository = companyProfileRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> users(String search, String status, String accountType) {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(user -> matchesUser(user, search, status, accountType))
                .map(this::toUserMap)
                .toList();
    }

    @Transactional
    public Map<String, Object> updateUserStatus(UUID userId, boolean active, Principal principal) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceConflictException("User not found"));
        user.setStatus(active ? UserStatus.ACTIVE : UserStatus.SUSPENDED);
        User saved = userRepository.save(user);
        writeAudit(principal, "ADMIN_USER_STATUS_UPDATED", "USER", saved.getId(), Map.of("active", active));
        return toUserMap(saved);
    }

    public List<Map<String, Object>> roles() {
        ensureDefaultPermissions();
        return roleRepository.findAll().stream().map(this::toRoleMap).toList();
    }

    @Transactional
    public Map<String, Object> createRole(Map<String, Object> payload, Principal principal) {
        String name = normalizeRoleName((String) payload.get("name"));
        if (roleRepository.findByName(name).isPresent()) {
            throw new ResourceConflictException("Role already exists");
        }
        Role role = new Role();
        role.setName(name);
        Role savedRole = roleRepository.save(role);
        syncPermissions(savedRole, extractPermissions(payload));
        writeAudit(principal, "ADMIN_ROLE_CREATED", "ROLE", savedRole.getId(), payload);
        return toRoleMap(savedRole);
    }

    @Transactional
    public Map<String, Object> updateRole(UUID roleId, Map<String, Object> payload, Principal principal) {
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new ResourceConflictException("Role not found"));
        if (payload.get("name") instanceof String name && !name.isBlank()) {
            role.setName(normalizeRoleName(name));
        }
        Role savedRole = roleRepository.save(role);
        syncPermissions(savedRole, extractPermissions(payload));
        writeAudit(principal, "ADMIN_ROLE_UPDATED", "ROLE", savedRole.getId(), payload);
        return toRoleMap(savedRole);
    }

    @Transactional
    public Map<String, String> deleteRole(UUID roleId, Principal principal) {
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new ResourceConflictException("Role not found"));
        if (Set.of("ROLE_STUDENT", "ROLE_COMPANY", "ROLE_ADMIN").contains(role.getName())) {
            throw new ResourceConflictException("Built-in roles cannot be deleted");
        }
        rolePermissionRepository.deleteByRoleId(roleId);
        roleRepository.delete(role);
        writeAudit(principal, "ADMIN_ROLE_DELETED", "ROLE", roleId, Map.of("name", role.getName()));
        return Map.of("message", "Role deleted");
    }

    public List<Map<String, Object>> pendingBursaries() {
        return bursaryRepository.findByStatusIgnoreCaseOrderByCreatedAtDesc("PENDING_APPROVAL").stream()
                .map(this::toBursaryModerationMap)
                .toList();
    }

    @Transactional
    public Map<String, Object> reviewBursary(UUID bursaryId, String decision, String comment, Principal principal) {
        Bursary bursary = bursaryRepository.findById(bursaryId).orElseThrow(() -> new ResourceConflictException("Bursary not found"));
        String normalized = decision == null ? "" : decision.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("APPROVED", "REJECTED", "REQUEST_CHANGES").contains(normalized)) {
            throw new ResourceConflictException("Invalid bursary review decision");
        }
        bursary.setStatus(switch (normalized) {
            case "APPROVED" -> "ACTIVE";
            case "REJECTED" -> "REJECTED";
            default -> "PENDING_APPROVAL";
        });
        Bursary saved = bursaryRepository.save(bursary);
        writeAudit(principal, "ADMIN_BURSARY_REVIEWED", "BURSARY", saved.getId(), Map.of("decision", normalized, "comment", comment));
        return toBursaryModerationMap(saved);
    }

    public Map<String, Object> analytics() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long totalCompanies = companyProfileRepository.count();
        long pendingCompanies = companyProfileRepository.countByStatus(CompanyApprovalStatus.PENDING);
        long activeBursaries = bursaryRepository.countByStatusIgnoreCase("ACTIVE");
        long closedBursaries = bursaryRepository.countByStatusIgnoreCase("CLOSED");
        long archivedBursaries = bursaryRepository.countByStatusIgnoreCase("ARCHIVED");
        long pendingApprovals = bursaryRepository.countByStatusIgnoreCase("PENDING_APPROVAL");
        List<SubscriptionRecord> subscriptions = subscriptionRepository.findAllByOrderByCreatedAtDesc();
        List<PaymentRecord> payments = paymentRepository.findAllByOrderByCreatedAtDesc();
        return Map.of(
                "totalUsers", totalUsers,
                "activeUsers", activeUsers,
                "totalCompanyAccounts", totalCompanies,
                "pendingCompanyApprovals", pendingCompanies,
                "bursaries", Map.of(
                        "active", activeBursaries,
                        "closed", closedBursaries,
                        "archived", archivedBursaries,
                        "pendingApproval", pendingApprovals,
                        "totalApplications", applicationRepository.count()
                ),
                "subscriptions", Map.of(
                        "active", subscriptionRepository.countByStatus("ACTIVE"),
                        "expired", subscriptionRepository.countByStatus("EXPIRED"),
                        "cancelled", subscriptionRepository.countByStatus("CANCELLED"),
                        "total", subscriptions.size()
                ),
                "payments", Map.of(
                        "completed", paymentRepository.countByStatus("COMPLETED"),
                        "pending", paymentRepository.countByStatus("PENDING"),
                        "failed", paymentRepository.countByStatus("FAILED"),
                        "total", payments.size()
                )
        );
    }

    public List<Map<String, Object>> auditLogs() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc().stream().map(log -> Map.<String, Object>of(
                "id", log.getId(),
                "actorId", log.getActorId(),
                "action", log.getAction(),
                "entityType", log.getEntityType(),
                "entityId", log.getEntityId(),
                "details", log.getDetails(),
                "createdAt", log.getCreatedAt()
        )).toList();
    }

    @Transactional
    public Map<String, Object> bulkUploadUsers(MultipartFile file, Principal principal) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ResourceConflictException("A CSV file is required");
        }
        List<Map<String, String>> created = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) {
                throw new ResourceConflictException("The uploaded file is empty");
            }
            String[] columns = header.split(",");
            if (columns.length < 5) {
                throw new ResourceConflictException("CSV must include: email,firstName,lastName,role,password");
            }
            String line;
            int row = 1;
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(",", -1);
                if (parts.length < 5) {
                    errors.add("Row %d is incomplete".formatted(row));
                    continue;
                }
                String email = parts[0].trim().toLowerCase(Locale.ROOT);
                String firstName = parts[1].trim();
                String lastName = parts[2].trim();
                String roleName = normalizeRoleName(parts[3].trim());
                String password = parts[4].trim();
                if (email.isBlank() || firstName.isBlank() || lastName.isBlank() || password.length() < 8) {
                    errors.add("Row %d failed validation".formatted(row));
                    continue;
                }
                if (userRepository.existsByEmail(email)) {
                    errors.add("Row %d duplicate email: %s".formatted(row, email));
                    continue;
                }
                Role role = roleRepository.findByName(roleName).orElse(null);
                if (role == null) {
                    errors.add("Row %d unknown role: %s".formatted(row, roleName));
                    continue;
                }
                User user = new User();
                user.setEmail(email);
                user.setFirstName(firstName);
                user.setLastName(lastName);
                user.setPasswordHash(passwordEncoder.encode(password));
                user.setStatus(UserStatus.ACTIVE);
                user.getRoles().add(role);
                User saved = userRepository.save(user);
                created.add(Map.of("id", saved.getId().toString(), "email", saved.getEmail(), "role", roleName));
            }
        }
        writeAudit(principal, "ADMIN_BULK_USER_UPLOAD", "USER", null, Map.of("createdCount", created.size(), "errorCount", errors.size()));
        return Map.of("created", created, "errors", errors, "createdCount", created.size(), "errorCount", errors.size());
    }

    private boolean matchesUser(User user, String search, String status, String accountType) {
        boolean searchMatches = search == null || search.isBlank()
                || user.getEmail().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))
                || (user.getFirstName() + " " + user.getLastName()).toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
        boolean statusMatches = status == null || status.isBlank() || user.getStatus().name().equalsIgnoreCase(status);
        boolean roleMatches = accountType == null || accountType.isBlank()
                || user.getRoles().stream().anyMatch(role -> role.getName().equalsIgnoreCase(normalizeRoleName(accountType)));
        return searchMatches && statusMatches && roleMatches;
    }

    private Map<String, Object> toUserMap(User user) {
        List<String> roles = user.getRoles().stream().map(Role::getName).sorted().toList();
        return new LinkedHashMap<>(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "fullName", (user.getFirstName() + " " + user.getLastName()).trim(),
                "role", roles.isEmpty() ? "" : roles.get(0).replace("ROLE_", ""),
                "roles", roles,
                "status", user.getStatus().name(),
                "active", user.getStatus() == UserStatus.ACTIVE,
                "createdAt", user.getCreatedAt()
        ));
    }

    private Map<String, Object> toRoleMap(Role role) {
        List<String> permissions = rolePermissionRepository.findByRoleIdOrderByPermissionCodeAsc(role.getId()).stream()
                .filter(RolePermission::isActive)
                .map(RolePermission::getPermissionCode)
                .toList();
        return new LinkedHashMap<>(Map.of(
                "id", role.getId(),
                "name", role.getName(),
                "permissions", permissions,
                "active", true
        ));
    }

    private Map<String, Object> toBursaryModerationMap(Bursary bursary) {
        return new LinkedHashMap<>(Map.of(
                "id", bursary.getId(),
                "title", bursary.getTitle(),
                "status", bursary.getStatus(),
                "applicationEndDate", bursary.getApplicationEndDate(),
                "applicantCount", applicationRepository.countByBursaryId(bursary.getId())
        ));
    }

    private void syncPermissions(Role role, List<String> requestedPermissions) {
        rolePermissionRepository.deleteByRoleId(role.getId());
        List<String> permissions = requestedPermissions.isEmpty() ? DEFAULT_ROLE_PERMISSIONS : requestedPermissions;
        for (String permission : new LinkedHashSet<>(permissions)) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(role.getId());
            rp.setPermissionCode(permission);
            rp.setActive(true);
            rolePermissionRepository.save(rp);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractPermissions(Map<String, Object> payload) {
        Object raw = payload.get("permissions");
        if (raw instanceof List<?> values) {
            return values.stream().map(String::valueOf).map(String::trim).filter(v -> !v.isBlank()).toList();
        }
        return List.of();
    }

    private String normalizeRoleName(String value) {
        if (value == null || value.isBlank()) {
            throw new ResourceConflictException("Role name is required");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }

    private void ensureDefaultPermissions() {
        for (String roleName : List.of("ROLE_STUDENT", "ROLE_COMPANY", "ROLE_ADMIN")) {
            Role role = roleRepository.findByName(roleName).orElse(null);
            if (role != null && rolePermissionRepository.findByRoleIdOrderByPermissionCodeAsc(role.getId()).isEmpty()) {
                syncPermissions(role, DEFAULT_ROLE_PERMISSIONS);
            }
        }
    }

    private void writeAudit(Principal principal, String action, String entityType, UUID entityId, Object details) {
        AuditLog log = new AuditLog();
        try {
            log.setDetails(details == null ? null : objectMapper.writeValueAsString(details));
        } catch (JsonProcessingException ex) {
            log.setDetails("{}");
        }
        log.setActorId(principal == null ? null : currentUserService.requireUser(principal).getId());
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        auditLogRepository.save(log);
    }
}
