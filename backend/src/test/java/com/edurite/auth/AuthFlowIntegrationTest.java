package com.edurite.auth;

import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.company.repository.CompanyProfileRepository;
import com.edurite.user.entity.User;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
import com.edurite.security.service.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("edurite_test")
            .withUsername("edurite")
            .withPassword("edurite");

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.cache.type", () -> "none");
        registry.add("spring.data.redis.repositories.enabled", () -> false);
        registry.add("edurite.auth.seed.admin.email", () -> "admin@test.local");
        registry.add("edurite.auth.seed.admin.password", () -> "AdminPass@123");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JwtService jwtService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    StudentProfileRepository studentProfileRepository;

    @Autowired
    CompanyProfileRepository companyProfileRepository;


    @Test
    void arnoldStudentCanRegisterAndLoginEndToEnd() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Arnold Madamombe","email":"arnoldmadaz@gmail.com","password":"Arnold@123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value("arnoldmadaz@gmail.com"))
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_STUDENT"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"arnoldmadaz@gmail.com","password":"Arnold@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("arnoldmadaz@gmail.com"));
    }

    @Test
    void studentRegistrationSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Jane Student","email":"jane.student@example.com","password":"StrongPass@123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_STUDENT"));
    }


    @Test
    void studentRegistrationStoresExtendedProfileFieldsAndLoginStillWorks() throws Exception {
        String email = "extended.student@example.com";
        mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Lina","lastName":"Moyo","email":"%s","password":"StrongPass@123","interests":"Engineering","location":"Johannesburg","phone":"+27110000000","dateOfBirth":"2004-09-15","gender":"Female","qualificationLevel":"High School"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value(email));

        User createdUser = userRepository.findByEmail(email).orElseThrow();
        StudentProfile profile = studentProfileRepository.findByUserId(createdUser.getId()).orElseThrow();
        assertThat(profile.getFirstName()).isEqualTo("Lina");
        assertThat(profile.getLastName()).isEqualTo("Moyo");
        assertThat(profile.getInterests()).isEqualTo("Engineering");
        assertThat(profile.getLocation()).isEqualTo("Johannesburg");
        assertThat(profile.getDateOfBirth()).hasToString("2004-09-15");
        assertThat(profile.getGender()).isEqualTo("Female");
        assertThat(profile.getQualificationLevel()).isEqualTo("High School");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"StrongPass@123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_STUDENT"));
    }

    @Test
    void legacyStudentWithoutExtendedProfileFieldsCanStillLogin() throws Exception {
        String email = "legacy.student@example.com";
        registerStudent(email);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"StrongPass@123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    void companyRegistrationSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/company")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyName":"Acme Corp","email":"hr@acme.com","officialEmail":"hr@acme.com","contactPersonName":"Alex Recruiter","registrationNumber":"ACME-REG-001","password":"StrongPass@123","industry":"Tech"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_COMPANY"));
    }

    @Test
    void duplicateEmailRejected() throws Exception {
        String body = """
                {"fullName":"Jane Student","email":"duplicate@example.com","password":"StrongPass@123"}
                """;
        mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account with email 'duplicate@example.com' already exists"));
    }

    @Test
    void loginSuccessForStudent() throws Exception {
        registerStudent("student.login@example.com");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"student.login@example.com","password":"StrongPass@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_STUDENT"));
    }

    @Test
    void loginSuccessForCompany() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/company")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"companyName":"Contoso","email":"contact@contoso.com","officialEmail":"contact@contoso.com","contactPersonName":"Taylor Hiring","registrationNumber":"CONTOSO-REG-001","password":"StrongPass@123","industry":"Education"}
                        """)).andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"contact@contoso.com","password":"StrongPass@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_COMPANY"))
                .andExpect(jsonPath("$.user.companyName").value("Contoso"));
    }

    @Test
    void newlyRegisteredCompanyIsPendingCanViewPortalProfileButCannotPostBursaryUntilApproved() throws Exception {
        String email = "pending.company@example.com";
        registerCompany(email, "PENDING-COMPANY-001");
        String companyToken = loginAndGetAccessToken(email, "StrongPass@123");

        mockMvc.perform(get("/api/v1/companies/me")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.companyName").value("Secure Co"));

        mockMvc.perform(post("/api/v1/companies/bursaries")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bursaryName":"Pending Access Bursary",
                                  "description":"Awaiting approval test bursary",
                                  "fieldOfStudy":"Engineering",
                                  "academicLevel":"Undergraduate",
                                  "applicationStartDate":"2026-04-01",
                                  "applicationEndDate":"2026-06-30",
                                  "fundingAmount":15000,
                                  "benefits":"Tuition",
                                  "requiredSubjects":["Maths"],
                                  "minimumGrade":"70%",
                                  "demographics":["Women in STEM"],
                                  "location":"Johannesburg",
                                  "eligibilityFilters":["South African citizens"]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Company is awaiting admin approval"));
    }

    @Test
    void adminCanApproveCompanyAndApprovedCompanyCanManageBursaries() throws Exception {
        String email = "approved.company@example.com";
        registerCompany(email, "APPROVAL-COMPANY-001");
        String companyToken = loginAndGetAccessToken(email, "StrongPass@123");
        String adminToken = loginAndGetAccessToken("admin@test.local", "AdminPass@123");
        User companyUser = userRepository.findByEmail(email).orElseThrow();
        var companyProfile = companyProfileRepository.findByUserId(companyUser.getId()).orElseThrow();

        mockMvc.perform(patch("/api/v1/admin/companies/" + companyProfile.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"notes":"Verified registration documents"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewNotes").value("Verified registration documents"));

        mockMvc.perform(post("/api/v1/companies/bursaries")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bursaryName":"Approved Access Bursary",
                                  "description":"Approved company bursary",
                                  "fieldOfStudy":"Computer Science",
                                  "academicLevel":"Postgraduate",
                                  "applicationStartDate":"2026-04-01",
                                  "applicationEndDate":"2026-07-31",
                                  "fundingAmount":25000,
                                  "benefits":"Tuition and stipend",
                                  "requiredSubjects":["Algorithms","Statistics"],
                                  "minimumGrade":"75%",
                                  "demographics":["Underrepresented groups"],
                                  "location":"Cape Town",
                                  "eligibilityFilters":["AI-ready profile","South Africa"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bursaryName").value("Approved Access Bursary"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void companyForgotPasswordResetFlowAllowsLoginWithNewPassword() throws Exception {
        String email = "reset.company@example.com";
        registerCompany(email, "RESET-COMPANY-001");

        String forgotResponse = mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode forgotJson = objectMapper.readTree(forgotResponse);
        String message = forgotJson.get("message").asText();
        String token = message.substring(message.lastIndexOf(':') + 1).trim();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"UpdatedPass@123","confirmPassword":"UpdatedPass@123"}
                                """.formatted(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset complete"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"UpdatedPass@123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_COMPANY"));
    }

    @Test
    void companyLoginUsesCompanyRoleAndAuthoritiesEvenWhenDatabaseRoleWasIncorrect() throws Exception {
        String email = "legacy.company.role@example.com";
        registerCompany(email, "LEGACY-COMPANY-001");

        User companyUser = userRepository.findByEmail(email).orElseThrow();
        companyUser.getRoles().clear();
        companyUser.getRoles().add(roleRepository.findByName("ROLE_STUDENT").orElseThrow());
        userRepository.saveAndFlush(companyUser);

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"StrongPass@123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("COMPANY"))
                .andExpect(jsonPath("$.primaryRole").value("ROLE_COMPANY"))
                .andExpect(jsonPath("$.user.role").value("COMPANY"))
                .andExpect(jsonPath("$.user.primaryRole").value("ROLE_COMPANY"))
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_COMPANY"))
                .andExpect(jsonPath("$.user.approvalStatus").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = objectMapper.readTree(loginResponse).get("accessToken").asText();
        assertThat(jwtService.extractRoles(accessToken)).containsExactly("ROLE_COMPANY");
        assertThat(jwtService.extractRole(accessToken)).isEqualTo("COMPANY");

        mockMvc.perform(get("/api/v1/companies/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.companyName").value("Secure Co"));
    }

    @Test
    void loginSuccessForAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@test.local","password":"AdminPass@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.primaryRole").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.primaryRole").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_ADMIN"));
    }

    @Test
    void loginResponsePrimaryRoleMatchesJwtAndDatabaseRoleForAllBuiltInRoles() throws Exception {
        registerStudent("primary.role.student@example.com");
        registerCompany("primary.role.company@example.com", "PRIMARY-ROLE-001");

        String studentResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"primary.role.student@example.com","password":"StrongPass@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.primaryRole").value("ROLE_STUDENT"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String companyResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"primary.role.company@example.com","password":"StrongPass@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("COMPANY"))
                .andExpect(jsonPath("$.primaryRole").value("ROLE_COMPANY"))
                .andExpect(jsonPath("$.user.approvalStatus").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String adminResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@test.local","password":"AdminPass@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.primaryRole").value("ROLE_ADMIN"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(jwtService.extractRoles(objectMapper.readTree(studentResponse).get("accessToken").asText())).containsExactly("ROLE_STUDENT");
        assertThat(jwtService.extractRole(objectMapper.readTree(studentResponse).get("accessToken").asText())).isEqualTo("STUDENT");
        assertThat(jwtService.extractRoles(objectMapper.readTree(companyResponse).get("accessToken").asText())).containsExactly("ROLE_COMPANY");
        assertThat(jwtService.extractRole(objectMapper.readTree(companyResponse).get("accessToken").asText())).isEqualTo("COMPANY");
        assertThat(jwtService.extractRoles(objectMapper.readTree(adminResponse).get("accessToken").asText())).containsExactly("ROLE_ADMIN");
        assertThat(jwtService.extractRole(objectMapper.readTree(adminResponse).get("accessToken").asText())).isEqualTo("ADMIN");
    }

    @Test
    void wrongPasswordRejected() throws Exception {
        registerStudent("wrong.password@example.com");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"wrong.password@example.com","password":"WrongPass@123"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/careers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointAcceptsValidJwt() throws Exception {
        registerStudent("jwt.user@example.com");

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"jwt.user@example.com","password":"StrongPass@123"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(loginResponse);
        String accessToken = jsonNode.get("accessToken").asText();

        mockMvc.perform(get("/api/v1/careers")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void jwtContainsExactAuthenticatedRolesForStudentCompanyAndAdmin() throws Exception {
        registerStudent("jwt.student.roles@example.com");
        registerCompany("jwt.company.roles@example.com", "JWT-COMPANY-001");

        String studentToken = loginAndGetAccessToken("jwt.student.roles@example.com", "StrongPass@123");
        String companyToken = loginAndGetAccessToken("jwt.company.roles@example.com", "StrongPass@123");
        String adminToken = loginAndGetAccessToken("admin@test.local", "AdminPass@123");

        assertThat(jwtService.extractRoles(studentToken)).containsExactly("ROLE_STUDENT");
        assertThat(jwtService.extractRoles(companyToken)).containsExactly("ROLE_COMPANY");
        assertThat(jwtService.extractRoles(adminToken)).containsExactly("ROLE_ADMIN");
    }

    @Test
    void passwordStoredHashed() throws Exception {
        registerStudent("hash.check@example.com");

        User user = userRepository.findByEmail("hash.check@example.com").orElseThrow();
        assertThat(user.getPasswordHash()).isNotEqualTo("StrongPass@123");
        assertThat(user.getPasswordHash()).startsWith("$2");
    }

    @Test
    void studentCanAccessStudentDataEndpoints() throws Exception {
        registerStudent("student.data@example.com");
        String accessToken = loginAndGetAccessToken("student.data@example.com", "StrongPass@123");

        mockMvc.perform(get("/api/v1/student/dashboard")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/recommendations/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedCareers").isArray())
                .andExpect(jsonPath("$.suggestedBursaries").isArray())
                .andExpect(jsonPath("$.suggestedCoursesOrImprovements").isArray())
                .andExpect(jsonPath("$.profileImprovementTips").isArray())
                .andExpect(jsonPath("$.modelVersion").isString());

        mockMvc.perform(get("/api/v1/subscriptions/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").isString())
                .andExpect(jsonPath("$.status").isString());

        mockMvc.perform(get("/api/v1/student/settings")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inAppNotificationsEnabled").isBoolean())
                .andExpect(jsonPath("$.emailNotificationsEnabled").isBoolean())
                .andExpect(jsonPath("$.smsNotificationsEnabled").isBoolean());

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/applications/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void companyCannotAccessStudentOnlyDataEndpoints() throws Exception {
        registerCompany("security.company@example.com", "SEC-COMPANY-001");
        String accessToken = loginAndGetAccessToken("security.company@example.com", "StrongPass@123");

        mockMvc.perform(get("/api/v1/student/dashboard")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/recommendations/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/subscriptions/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/applications/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }


    @Test
    void adminCannotAccessCompanyEndpointsAndCompanyCannotAccessAdminEndpoints() throws Exception {
        registerCompany("cross.role.company@example.com", "CROSS-ROLE-001");
        String companyToken = loginAndGetAccessToken("cross.role.company@example.com", "StrongPass@123");
        String adminToken = loginAndGetAccessToken("admin@test.local", "AdminPass@123");

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/companies/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentDataEndpointsRejectMissingAuth() throws Exception {
        mockMvc.perform(get("/api/v1/student/dashboard")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/recommendations/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/subscriptions/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/student/settings")).andExpect(status().isUnauthorized());
    }

    private void registerStudent(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Test Student","email":"%s","password":"StrongPass@123"}
                                """.formatted(email)))
                .andExpect(status().isCreated());
    }

    private void registerCompany(String email, String registrationNumber) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/company")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyName":"Secure Co","email":"%s","officialEmail":"%s","contactPersonName":"Security Owner","registrationNumber":"%s","password":"StrongPass@123","industry":"Security"}
                                """.formatted(email, email, registrationNumber)))
                .andExpect(status().isCreated());
    }

    private String loginAndGetAccessToken(String email, String password) throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(loginResponse);
        return jsonNode.get("accessToken").asText();
    }
}
