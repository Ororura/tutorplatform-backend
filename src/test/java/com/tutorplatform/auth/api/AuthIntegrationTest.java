package com.tutorplatform.auth.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.user.domain.UserRole;
import com.tutorplatform.user.domain.TeacherRepository;
import com.tutorplatform.user.domain.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthIntegrationTest {

    private static final String PASSWORD = "correct horse battery staple";

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanIdentityData() {
        teacherRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void csrfEndpointReturnsTokenAndCreatesSession() throws Exception {
        CsrfExchange csrf = obtainCsrf();

        assertThat(csrf.token()).isNotBlank();
        assertThat(csrf.headerName()).isEqualTo("X-XSRF-TOKEN");
        assertThat(csrf.sessionCookie().getValue()).isNotBlank();
    }

    @Test
    void registerTeacherCreatesIdentityAtomicallyAndAuthenticatedSession() throws Exception {
        CsrfExchange csrf = obtainCsrf();

        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register/teacher")
                        .cookie(csrf.sessionCookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "  Егор  ",
                                  "email": "teacher@example.com",
                                  "password": "correct horse battery staple"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value("teacher@example.com"))
                .andExpect(jsonPath("$.displayName").value("Егор"))
                .andExpect(jsonPath("$.roles[0]").value("TEACHER"))
                .andReturn();

        var user = userRepository.findByEmail("TEACHER@example.com").orElseThrow();
        assertThat(user.getRoles()).containsExactly(UserRole.TEACHER);
        assertThat(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).isTrue();
        assertThat(user.getPasswordHash()).startsWith("$argon2");
        assertThat(teacherRepository.findByUserId(user.getId()))
                .get()
                .extracting(teacher -> teacher.getDisplayName())
                .isEqualTo("Егор");

        Cookie authenticatedSession = sessionCookieFrom(registration, csrf.sessionCookie());
        assertThat(authenticatedSession.getValue()).isNotEqualTo(csrf.sessionCookie().getValue());
        mockMvc.perform(get("/api/v1/auth/me").cookie(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value("teacher@example.com"))
                .andExpect(jsonPath("$.displayName").value("Егор"))
                .andExpect(jsonPath("$.roles[0]").value("TEACHER"));
    }

    @Test
    void duplicateEmailReturnsConflictWithoutPartialTeacher() throws Exception {
        register("teacher@example.com", "Егор");
        CsrfExchange csrf = obtainCsrf();

        mockMvc.perform(post("/api/v1/auth/register/teacher")
                        .cookie(csrf.sessionCookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson("TEACHER@example.com", "Другой")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));

        assertThat(userRepository.count()).isOne();
        assertThat(teacherRepository.count()).isOne();
    }

    @Test
    void registrationValidationReturnsStandardValidationError() throws Exception {
        CsrfExchange csrf = obtainCsrf();

        mockMvc.perform(post("/api/v1/auth/register/teacher")
                        .cookie(csrf.sessionCookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": " x ",
                                  "email": "not-an-email",
                                  "password": "too-short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.length()").value(3));

        assertThat(userRepository.count()).isZero();
        assertThat(teacherRepository.count()).isZero();
    }

    @Test
    void malformedJsonReturnsStandardValidationError() throws Exception {
        CsrfExchange csrf = obtainCsrf();

        mockMvc.perform(post("/api/v1/auth/register/teacher")
                        .cookie(csrf.sessionCookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void loginCreatesSessionAndUnknownEmailAndWrongPasswordReturnSameError() throws Exception {
        register("teacher@example.com", "Егор");

        CsrfExchange validCsrf = obtainCsrf();
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(validCsrf.sessionCookie())
                        .header(validCsrf.headerName(), validCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("TEACHER@example.com", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("teacher@example.com"))
                .andExpect(jsonPath("$.displayName").value("Егор"))
                .andExpect(jsonPath("$.roles[0]").value("TEACHER"))
                .andReturn();

        Cookie authenticatedSession = sessionCookieFrom(login, validCsrf.sessionCookie());
        assertThat(authenticatedSession.getValue()).isNotEqualTo(validCsrf.sessionCookie().getValue());
        mockMvc.perform(get("/api/v1/auth/me")
                        .cookie(authenticatedSession))
                .andExpect(status().isOk());

        assertInvalidCredentials("missing@example.com", PASSWORD);
        assertInvalidCredentials("teacher@example.com", "incorrect password value");
    }

    @Test
    void currentUserRequiresSessionAndLogoutInvalidatesIt() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        Cookie authenticatedSession = register("teacher@example.com", "Егор");
        CsrfExchange authenticatedCsrf = obtainCsrf(authenticatedSession);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(authenticatedCsrf.sessionCookie())
                        .header(authenticatedCsrf.headerName(), authenticatedCsrf.token()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("TUTOR_SESSION", 0));

        mockMvc.perform(get("/api/v1/auth/me").cookie(authenticatedCsrf.sessionCookie()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    void stateChangingAuthEndpointsRequireCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("teacher@example.com", PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    }

    @Test
    void openApiPublishesStableAuthOperationIdsAndSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/csrf'].get.operationId").value("getCsrfToken"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/register/teacher'].post.operationId").value("registerTeacher"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.operationId").value("login"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/logout'].post.operationId").value("logout"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/me'].get.operationId").value("getCurrentUser"))
                .andExpect(jsonPath("$.components.schemas.TeacherRegistrationRequest.properties.displayName.maxLength").value(160))
                .andExpect(jsonPath("$.components.schemas.CurrentUserResponse.properties.id.format").value("uuid"))
                .andExpect(jsonPath("$.components.schemas.CurrentUserResponse.properties.roles.type").value("array"))
                .andExpect(jsonPath("$.components.schemas.CurrentUserResponse.properties.roles.items.enum.length()").value(2))
                .andExpect(jsonPath("$.components.schemas.ApiError.properties.timestamp.format").value("date-time"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.responses['401'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ApiError"));
    }

    private Cookie register(String email, String displayName) throws Exception {
        CsrfExchange csrf = obtainCsrf();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register/teacher")
                        .cookie(csrf.sessionCookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(email, displayName)))
                .andExpect(status().isCreated())
                .andReturn();
        return sessionCookieFrom(result, csrf.sessionCookie());
    }

    private void assertInvalidCredentials(String email, String password) throws Exception {
        CsrfExchange csrf = obtainCsrf();
        mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrf.sessionCookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, password)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    private CsrfExchange obtainCsrf() throws Exception {
        return obtainCsrf(null);
    }

    private CsrfExchange obtainCsrf(Cookie existingSession) throws Exception {
        var request = get("/api/v1/auth/csrf");
        if (existingSession != null) {
            request.cookie(existingSession);
        }

        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return new CsrfExchange(
                body.required("headerName").textValue(),
                body.required("token").textValue(),
                sessionCookieFrom(result, existingSession)
        );
    }

    private Cookie sessionCookieFrom(MvcResult result, Cookie fallback) {
        Cookie cookie = result.getResponse().getCookie("TUTOR_SESSION");
        if (cookie != null) {
            return cookie;
        }
        assertThat(fallback).as("existing session cookie").isNotNull();
        return fallback;
    }

    private String registrationJson(String email, String displayName) throws Exception {
        return objectMapper.writeValueAsString(new TeacherRegistrationRequest(displayName, email, PASSWORD));
    }

    private String loginJson(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(new LoginRequest(email, password));
    }

    private record CsrfExchange(String headerName, String token, Cookie sessionCookie) {
    }
}
