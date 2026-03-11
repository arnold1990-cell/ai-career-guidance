package com.edurite.auth;

import com.edurite.user.entity.User;
import com.edurite.user.repository.UserRepository;
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
    UserRepository userRepository;


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
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_COMPANY"));
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
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_ADMIN"));
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
