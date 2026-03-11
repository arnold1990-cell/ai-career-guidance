package com.edurite.auth; // declares the package path for this Java file

import com.edurite.user.entity.User; // imports a class so it can be used in this file
import com.edurite.user.repository.UserRepository; // imports a class so it can be used in this file
import com.fasterxml.jackson.databind.JsonNode; // imports a class so it can be used in this file
import com.fasterxml.jackson.databind.ObjectMapper; // imports a class so it can be used in this file
import org.junit.jupiter.api.Test; // imports a class so it can be used in this file
import org.springframework.beans.factory.annotation.Autowired; // imports a class so it can be used in this file
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; // imports a class so it can be used in this file
import org.springframework.boot.test.context.SpringBootTest; // imports a class so it can be used in this file
import org.springframework.http.MediaType; // imports a class so it can be used in this file
import org.springframework.test.context.ActiveProfiles; // imports a class so it can be used in this file
import org.springframework.test.context.DynamicPropertyRegistry; // imports a class so it can be used in this file
import org.springframework.test.context.DynamicPropertySource; // imports a class so it can be used in this file
import org.springframework.test.web.servlet.MockMvc; // imports a class so it can be used in this file
import org.testcontainers.containers.PostgreSQLContainer; // imports a class so it can be used in this file
import org.testcontainers.junit.jupiter.Container; // imports a class so it can be used in this file
import org.testcontainers.junit.jupiter.Testcontainers; // imports a class so it can be used in this file

import static org.assertj.core.api.Assertions.assertThat; // imports a class so it can be used in this file
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get; // imports a class so it can be used in this file
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post; // imports a class so it can be used in this file
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath; // imports a class so it can be used in this file
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status; // imports a class so it can be used in this file

@SpringBootTest // adds metadata that Spring or Java uses at runtime
@AutoConfigureMockMvc // adds metadata that Spring or Java uses at runtime
@Testcontainers // adds metadata that Spring or Java uses at runtime
@ActiveProfiles("test") // adds metadata that Spring or Java uses at runtime
class AuthFlowIntegrationTest { // defines a class type

    @Container // adds metadata that Spring or Java uses at runtime
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine") // creates a new object instance and stores it in a variable
            .withDatabaseName("edurite_test") // supports the surrounding application logic
            .withUsername("edurite") // supports the surrounding application logic
            .withPassword("edurite"); // executes this statement as part of the application logic

    @DynamicPropertySource // adds metadata that Spring or Java uses at runtime
    static void dynamicProperties(DynamicPropertyRegistry registry) { // supports the surrounding application logic
        registry.add("spring.datasource.url", postgres::getJdbcUrl); // reads or writes data through the database layer
        registry.add("spring.datasource.username", postgres::getUsername); // executes this statement as part of the application logic
        registry.add("spring.datasource.password", postgres::getPassword); // executes this statement as part of the application logic
        registry.add("spring.flyway.enabled", () -> true); // executes this statement as part of the application logic
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate"); // executes this statement as part of the application logic
        registry.add("spring.cache.type", () -> "none"); // executes this statement as part of the application logic
        registry.add("spring.data.redis.repositories.enabled", () -> false); // executes this statement as part of the application logic
        registry.add("edurite.auth.seed.admin.email", () -> "admin@test.local"); // executes this statement as part of the application logic
        registry.add("edurite.auth.seed.admin.password", () -> "AdminPass@123"); // executes this statement as part of the application logic
    } // ends the current code block

    @Autowired // asks Spring to inject this dependency automatically
    MockMvc mockMvc; // executes this statement as part of the application logic

    @Autowired // asks Spring to inject this dependency automatically
    ObjectMapper objectMapper; // executes this statement as part of the application logic

    @Autowired // asks Spring to inject this dependency automatically
    UserRepository userRepository; // reads or writes data through the database layer


    @Test // adds metadata that Spring or Java uses at runtime
    void arnoldStudentCanRegisterAndLoginEndToEnd() throws Exception { // supports the surrounding application logic
        mockMvc.perform(post("/api/v1/auth/register/student") // supports the surrounding application logic
                        .contentType(MediaType.APPLICATION_JSON) // supports the surrounding application logic
                        .content(""" // supports the surrounding application logic
                                {"fullName":"Arnold Madamombe","email":"arnoldmadaz@gmail.com","password":"Arnold@123"} // supports the surrounding application logic
                                """)) // supports the surrounding application logic
                .andExpect(status().isCreated()) // supports the surrounding application logic
                .andExpect(jsonPath("$.user.email").value("arnoldmadaz@gmail.com")) // supports the surrounding application logic
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_STUDENT")); // executes this statement as part of the application logic

        mockMvc.perform(post("/api/v1/auth/login") // supports the surrounding application logic
                        .contentType(MediaType.APPLICATION_JSON) // supports the surrounding application logic
                        .content(""" // supports the surrounding application logic
                                {"email":"arnoldmadaz@gmail.com","password":"Arnold@123"} // supports the surrounding application logic
                                """)) // supports the surrounding application logic
                .andExpect(status().isOk()) // supports the surrounding application logic
                .andExpect(jsonPath("$.accessToken").isNotEmpty()) // handles authentication or authorization to protect secure access
                .andExpect(jsonPath("$.user.email").value("arnoldmadaz@gmail.com")); // executes this statement as part of the application logic
    } // ends the current code block

    @Test // adds metadata that Spring or Java uses at runtime
    void studentRegistrationSuccess() throws Exception { // supports the surrounding application logic
        mockMvc.perform(post("/api/v1/auth/register/student") // supports the surrounding application logic
                        .contentType(MediaType.APPLICATION_JSON) // supports the surrounding application logic
                        .content(""" // supports the surrounding application logic
                                {"fullName":"Jane Student","email":"jane.student@example.com","password":"StrongPass@123"} // supports the surrounding application logic
                                """)) // supports the surrounding application logic
                .andExpect(status().isCreated()) // supports the surrounding application logic
                .andExpect(jsonPath("$.accessToken").isNotEmpty()) // handles authentication or authorization to protect secure access
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_STUDENT")); // executes this statement as part of the application logic
    } // ends the current code block

    @Test // adds metadata that Spring or Java uses at runtime
    void companyRegistrationSuccess() throws Exception { // supports the surrounding application logic
        mockMvc.perform(post("/api/v1/auth/register/company") // supports the surrounding application logic
                        .contentType(MediaType.APPLICATION_JSON) // supports the surrounding application logic
                        .content(""" // supports the surrounding application logic
                                {"companyName":"Acme Corp","email":"hr@acme.com","officialEmail":"hr@acme.com","contactPersonName":"Alex Recruiter","registrationNumber":"ACME-REG-001","password":"StrongPass@123","industry":"Tech"} // supports the surrounding application logic
                                """)) // supports the surrounding application logic
                .andExpect(status().isCreated()) // supports the surrounding application logic
                .andExpect(jsonPath("$.accessToken").isNotEmpty()) // handles authentication or authorization to protect secure access
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_COMPANY")); // executes this statement as part of the application logic
    } // ends the current code block

    @Test // adds metadata that Spring or Java uses at runtime
    void duplicateEmailRejected() throws Exception { // supports the surrounding application logic
        String body = """ // supports the surrounding application logic
                {"fullName":"Jane Student","email":"duplicate@example.com","password":"StrongPass@123"} // supports the surrounding application logic
                """; // executes this statement as part of the application logic
        mockMvc.perform(post("/api/v1/auth/register/student") // supports the surrounding application logic
                        .contentType(MediaType.APPLICATION_JSON) // supports the surrounding application logic
                        .content(body)) // supports the surrounding application logic
                .andExpect(status().isCreated()); // executes this statement as part of the application logic

        mockMvc.perform(post("/api/v1/auth/register/student") // supports the surrounding application logic
                        .contentType(MediaType.APPLICATION_JSON) // supports the surrounding application logic
                        .content(body)) // supports the surrounding application logic
                .andExpect(status().isConflict()) // supports the surrounding application logic
                .andExpect(jsonPath("$.message").value("An account with email 'duplicate@example.com' already exists")); // executes this statement as part of the application logic
    } // ends the current code block

    @Test // adds metadata that Spring or Java uses at runtime
    void loginSuccessForStudent() throws Exception { // supports the surrounding application logic
        registerStudent("student.login@example.com"); // executes this statement as part of the application logic

        mockMvc.perform(post("/api/v1/auth/login") // supports the surrounding application logic
                        .contentType(MediaType.APPLICATION_JSON) // supports the surrounding application logic
                        .content(""" // supports the surrounding application logic
                                {"email":"student.login@example.com","password":"StrongPass@123"} // supports the surrounding application logic
                                """)) // supports the surrounding application logic
                .andExpect(status().isOk()) // supports the surrounding application logic
                .andExpect(jsonPath("$.accessToken").isNotEmpty()) // handles authentication or authorization to protect secure access
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_STUDENT")); // executes this statement as part of the application logic
    } // ends the current code block

    @Test // adds metadata that Spring or Java uses at runtime
    void loginSuccessForCompany() throws Exception { // supports the surrounding application logic
        mockMvc.perform(post("/api/v1/auth/register/company") // supports the surrounding application logic
                .contentType(MediaType.APPLICATION_JSON) // supports the surrounding application logic
                .content(""" // supports the surrounding application logic
                        {"companyName":"Contoso","email":"contact@contoso.com","officialEmail":"contact@contoso.com","contactPersonName":"Taylor Hiring","registrationNumber":"CONTOSO-REG-001","password":"StrongPass@123","industry":"Education"} // supports the surrounding application logic
                        """)).andExpect(status().isCreated()); // executes this statement as part of the application logic

        mockMvc.perform(post("/api/v1/auth/login") // supports the surrounding application logic
                        .contentType(MediaType.APPLICATION_JSON) // supports the surrounding application logic
                        .content(""" // supports the surrounding application logic
                                {"email":"contact@contoso.com","password":"StrongPass@123"} // supports the surrounding application logic
                                """)) // supports the surrounding application logic
                .andExpect(status().isOk()) // supports the surrounding application logic
                .andExpect(jsonPath("$.accessToken").isNotEmpty()) // handles authentication or authorization to protect secure access
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_COMPANY")); // executes this statement as part of the application logic
    } // ends the current code block

    @Test // adds metadata that Spring or Java uses at runtime
    void loginSuccessForAdmin() throws Exception { // supports the surrounding application logic
        mockMvc.perform(post("/api/v1/auth/login") // supports the surrounding application logic
                        .contentType(MediaType.APPLICATION_JSON) // supports the surrounding application logic
                        .content(""" // supports the surrounding application logic
                                {"email":"admin@test.local","password":"AdminPass@123"} // supports the surrounding application logic
                                """)) // supports the surrounding application logic
                .andExpect(status().isOk()) // supports the surrounding application logic
                .andExpect(jsonPath("$.accessToken").isNotEmpty()) // handles authentication or authorization to protect secure access
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_ADMIN")); // executes this statement as part of the application logic
    } // ends the current code block

    @Test // adds metadata that Spring or Java uses at runtime
    void wrongPasswordRejected() throws Exception { // supports the surrounding application logic
        registerStudent("wrong.password@example.com"); // executes this statement as part of the application logic

        mockMvc.perform(post("/api/v1/auth/login") // supports the surrounding application logic
                        .contentType(MediaType.APPLICATION_JSON) // supports the surrounding application logic
                        .content(""" // supports the surrounding application logic
                                {"email":"wrong.password@example.com","password":"WrongPass@123"} // supports the surrounding application logic
                                """)) // supports the surrounding application logic
                .andExpect(status().isUnauthorized()); // executes this statement as part of the application logic
    } // ends the current code block

    @Test // adds metadata that Spring or Java uses at runtime
    void protectedEndpointRejectsUnauthenticated() throws Exception { // handles authentication or authorization to protect secure access
        mockMvc.perform(get("/api/v1/careers")) // supports the surrounding application logic
                .andExpect(status().isUnauthorized()); // executes this statement as part of the application logic
    } // ends the current code block

    @Test // adds metadata that Spring or Java uses at runtime
    void protectedEndpointAcceptsValidJwt() throws Exception { // handles authentication or authorization to protect secure access
        registerStudent("jwt.user@example.com"); // handles authentication or authorization to protect secure access

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login") // supports the surrounding application logic
                        .contentType(MediaType.APPLICATION_JSON) // supports the surrounding application logic
                        .content(""" // supports the surrounding application logic
                                {"email":"jwt.user@example.com","password":"StrongPass@123"} // handles authentication or authorization to protect secure access
                                """)) // supports the surrounding application logic
                .andExpect(status().isOk()) // supports the surrounding application logic
                .andReturn() // supports the surrounding application logic
                .getResponse() // supports the surrounding application logic
                .getContentAsString(); // executes this statement as part of the application logic

        JsonNode jsonNode = objectMapper.readTree(loginResponse); // executes this statement as part of the application logic
        String accessToken = jsonNode.get("accessToken").asText(); // handles authentication or authorization to protect secure access

        mockMvc.perform(get("/api/v1/careers") // supports the surrounding application logic
                        .header("Authorization", "Bearer " + accessToken)) // handles authentication or authorization to protect secure access
                .andExpect(status().isOk()); // executes this statement as part of the application logic
    } // ends the current code block

    @Test // adds metadata that Spring or Java uses at runtime
    void passwordStoredHashed() throws Exception { // supports the surrounding application logic
        registerStudent("hash.check@example.com"); // executes this statement as part of the application logic

        User user = userRepository.findByEmail("hash.check@example.com").orElseThrow(); // reads or writes data through the database layer
        assertThat(user.getPasswordHash()).isNotEqualTo("StrongPass@123"); // executes this statement as part of the application logic
        assertThat(user.getPasswordHash()).startsWith("$2"); // executes this statement as part of the application logic
    } // ends the current code block

    @Test // adds metadata that Spring or Java uses at runtime
    void studentCanAccessStudentDataEndpoints() throws Exception { // supports the surrounding application logic
        registerStudent("student.data@example.com"); // executes this statement as part of the application logic
        String accessToken = loginAndGetAccessToken("student.data@example.com", "StrongPass@123"); // handles authentication or authorization to protect secure access

        mockMvc.perform(get("/api/v1/student/dashboard") // supports the surrounding application logic
                        .header("Authorization", "Bearer " + accessToken)) // handles authentication or authorization to protect secure access
                .andExpect(status().isOk()); // executes this statement as part of the application logic

        mockMvc.perform(get("/api/v1/recommendations/me") // supports the surrounding application logic
                        .header("Authorization", "Bearer " + accessToken)) // handles authentication or authorization to protect secure access
                .andExpect(status().isOk()) // supports the surrounding application logic
                .andExpect(jsonPath("$.suggestedCareers").isArray()) // supports the surrounding application logic
                .andExpect(jsonPath("$.suggestedBursaries").isArray()) // supports the surrounding application logic
                .andExpect(jsonPath("$.suggestedCoursesOrImprovements").isArray()) // supports the surrounding application logic
                .andExpect(jsonPath("$.profileImprovementTips").isArray()) // supports the surrounding application logic
                .andExpect(jsonPath("$.modelVersion").isString()); // executes this statement as part of the application logic

        mockMvc.perform(get("/api/v1/subscriptions/me") // supports the surrounding application logic
                        .header("Authorization", "Bearer " + accessToken)) // handles authentication or authorization to protect secure access
                .andExpect(status().isOk()) // supports the surrounding application logic
                .andExpect(jsonPath("$.planCode").isString()) // supports the surrounding application logic
                .andExpect(jsonPath("$.status").isString()); // executes this statement as part of the application logic

        mockMvc.perform(get("/api/v1/student/settings") // supports the surrounding application logic
                        .header("Authorization", "Bearer " + accessToken)) // handles authentication or authorization to protect secure access
                .andExpect(status().isOk()) // supports the surrounding application logic
                .andExpect(jsonPath("$.inAppNotificationsEnabled").isBoolean()) // supports the surrounding application logic
                .andExpect(jsonPath("$.emailNotificationsEnabled").isBoolean()) // supports the surrounding application logic
                .andExpect(jsonPath("$.smsNotificationsEnabled").isBoolean()); // executes this statement as part of the application logic

        mockMvc.perform(get("/api/v1/notifications") // supports the surrounding application logic
                        .header("Authorization", "Bearer " + accessToken)) // handles authentication or authorization to protect secure access
                .andExpect(status().isOk()); // executes this statement as part of the application logic

        mockMvc.perform(get("/api/v1/applications/me") // supports the surrounding application logic
                        .header("Authorization", "Bearer " + accessToken)) // handles authentication or authorization to protect secure access
                .andExpect(status().isOk()); // executes this statement as part of the application logic
    } // ends the current code block

    @Test // adds metadata that Spring or Java uses at runtime
    void companyCannotAccessStudentOnlyDataEndpoints() throws Exception { // supports the surrounding application logic
        registerCompany("security.company@example.com", "SEC-COMPANY-001"); // executes this statement as part of the application logic
        String accessToken = loginAndGetAccessToken("security.company@example.com", "StrongPass@123"); // handles authentication or authorization to protect secure access

        mockMvc.perform(get("/api/v1/student/dashboard") // supports the surrounding application logic
                        .header("Authorization", "Bearer " + accessToken)) // handles authentication or authorization to protect secure access
                .andExpect(status().isForbidden()); // executes this statement as part of the application logic

        mockMvc.perform(get("/api/v1/recommendations/me") // supports the surrounding application logic
                        .header("Authorization", "Bearer " + accessToken)) // handles authentication or authorization to protect secure access
                .andExpect(status().isForbidden()); // executes this statement as part of the application logic

        mockMvc.perform(get("/api/v1/subscriptions/me") // supports the surrounding application logic
                        .header("Authorization", "Bearer " + accessToken)) // handles authentication or authorization to protect secure access
                .andExpect(status().isForbidden()); // executes this statement as part of the application logic

        mockMvc.perform(get("/api/v1/notifications") // supports the surrounding application logic
                        .header("Authorization", "Bearer " + accessToken)) // handles authentication or authorization to protect secure access
                .andExpect(status().isForbidden()); // executes this statement as part of the application logic

        mockMvc.perform(get("/api/v1/applications/me") // supports the surrounding application logic
                        .header("Authorization", "Bearer " + accessToken)) // handles authentication or authorization to protect secure access
                .andExpect(status().isForbidden()); // executes this statement as part of the application logic
    } // ends the current code block

    @Test // adds metadata that Spring or Java uses at runtime
    void studentDataEndpointsRejectMissingAuth() throws Exception { // supports the surrounding application logic
        mockMvc.perform(get("/api/v1/student/dashboard")).andExpect(status().isUnauthorized()); // executes this statement as part of the application logic
        mockMvc.perform(get("/api/v1/recommendations/me")).andExpect(status().isUnauthorized()); // executes this statement as part of the application logic
        mockMvc.perform(get("/api/v1/subscriptions/me")).andExpect(status().isUnauthorized()); // executes this statement as part of the application logic
        mockMvc.perform(get("/api/v1/student/settings")).andExpect(status().isUnauthorized()); // executes this statement as part of the application logic
    } // ends the current code block

    private void registerStudent(String email) throws Exception { // declares a method that defines behavior for this class
        mockMvc.perform(post("/api/v1/auth/register/student") // supports the surrounding application logic
                        .contentType(MediaType.APPLICATION_JSON) // supports the surrounding application logic
                        .content(""" // supports the surrounding application logic
                                {"fullName":"Test Student","email":"%s","password":"StrongPass@123"} // supports the surrounding application logic
                                """.formatted(email))) // supports the surrounding application logic
                .andExpect(status().isCreated()); // executes this statement as part of the application logic
    } // ends the current code block

    private void registerCompany(String email, String registrationNumber) throws Exception { // declares a method that defines behavior for this class
        mockMvc.perform(post("/api/v1/auth/register/company") // supports the surrounding application logic
                        .contentType(MediaType.APPLICATION_JSON) // supports the surrounding application logic
                        .content(""" // supports the surrounding application logic
                                {"companyName":"Secure Co","email":"%s","officialEmail":"%s","contactPersonName":"Security Owner","registrationNumber":"%s","password":"StrongPass@123","industry":"Security"} // supports the surrounding application logic
                                """.formatted(email, email, registrationNumber))) // supports the surrounding application logic
                .andExpect(status().isCreated()); // executes this statement as part of the application logic
    } // ends the current code block

    private String loginAndGetAccessToken(String email, String password) throws Exception { // handles authentication or authorization to protect secure access
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login") // supports the surrounding application logic
                        .contentType(MediaType.APPLICATION_JSON) // supports the surrounding application logic
                        .content(""" // supports the surrounding application logic
                                {"email":"%s","password":"%s"} // supports the surrounding application logic
                                """.formatted(email, password))) // supports the surrounding application logic
                .andExpect(status().isOk()) // supports the surrounding application logic
                .andReturn() // supports the surrounding application logic
                .getResponse() // supports the surrounding application logic
                .getContentAsString(); // executes this statement as part of the application logic

        JsonNode jsonNode = objectMapper.readTree(loginResponse); // executes this statement as part of the application logic
        return jsonNode.get("accessToken").asText(); // returns a value from this method to the caller
    } // ends the current code block
} // ends the current code block
