package com.tutorplatform.auth.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.test.PostgresIntegrationTest;
import com.tutorplatform.user.domain.TeacherEntity;
import com.tutorplatform.user.domain.TeacherRepository;
import com.tutorplatform.user.domain.UserEntity;
import com.tutorplatform.user.domain.UserRepository;
import com.tutorplatform.user.domain.UserRole;
import com.tutorplatform.user.domain.UserStatus;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class AuthRateLimitIntegrationTest extends PostgresIntegrationTest {
    private static final String PASSWORD = "correct horse battery staple";
    private static final String CLIENT = "192.0.2.1";
    private static final AtomicLong TIME = new AtomicLong();

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_auth_rate_limit", null);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcClient jdbcClient;
    @Autowired private UserRepository userRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @TestBean(methodName = "testLimiter")
    private AuthRateLimiter authRateLimiter;

    static AuthRateLimiter testLimiter() {
        return new AuthRateLimiter(AuthRateLimiterTest.properties(2, 100), TIME::get);
    }

    @BeforeEach
    void resetIdentityAndExpirePreviousWindows() {
        TIME.addAndGet(Duration.ofMinutes(11).toNanos());
        authRateLimiter.cleanup();
        jdbcClient
                .sql(
                        "UPDATE platform_settings SET registration_mode = 'OPEN', updated_by_admin_id = NULL")
                .update();
        teacherRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void belowLimitPreservesInvalidCredentialsAndWindowExpiryRestoresAccess() throws Exception {
        login(CLIENT, "missing@example.com", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
        login(CLIENT, "another@example.com", PASSWORD).andExpect(status().isUnauthorized());
        expectRateLimited(login(CLIENT, "third@example.com", PASSWORD));
        TIME.addAndGet(Duration.ofSeconds(59).toNanos());
        login(CLIENT, "missing@example.com", PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "1"));
        TIME.addAndGet(Duration.ofSeconds(1).toNanos());
        login(CLIENT, "missing@example.com", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing@example.com", "teacher@example.com"})
    void unknownEmailAndWrongPasswordHaveIdenticalErrorsAndQuota(String email) throws Exception {
        createTeacher();
        for (int i = 0; i < 2; i++) {
            login(CLIENT, email, "incorrect password value")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"))
                    .andExpect(jsonPath("$.message").value("Invalid email or password"))
                    .andExpect(jsonPath("$.details").isEmpty());
        }
        expectRateLimited(login(CLIENT, email, "incorrect password value"));
        assertThat(userRepository.count()).isOne();
    }

    @Test
    void successfulLoginRotatesSessionAndAuthenticatedCrudIsNotLimited() throws Exception {
        createTeacher();
        Csrf csrf = obtainCsrf();
        var result =
                perform(
                                post("/api/v1/auth/login"),
                                CLIENT,
                                loginBody("TEACHER@example.com", PASSWORD),
                                csrf)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.email").value("teacher@example.com"))
                        .andReturn();
        Cookie session = result.getResponse().getCookie("TUTOR_SESSION");
        assertThat(session).isNotNull();
        assertThat(session.getValue()).isNotEqualTo(csrf.cookie().getValue());
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/v1/auth/me").cookie(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roles[0]").value("TEACHER"));
            var request =
                    get("/api/v1/teacher/students")
                            .cookie(session)
                            .with(
                                    servlet -> {
                                        servlet.setRemoteAddr(CLIENT);
                                        return servlet;
                                    });
            mockMvc.perform(request).andExpect(status().isOk());
        }
        login(CLIENT, "teacher@example.com", "incorrect password value")
                .andExpect(status().isUnauthorized());
        expectRateLimited(login(CLIENT, "teacher@example.com", PASSWORD));
        mockMvc.perform(get("/api/v1/auth/me").cookie(session)).andExpect(status().isOk());
        var tokenResult =
                mockMvc.perform(get("/api/v1/auth/csrf").cookie(session))
                        .andExpect(status().isOk())
                        .andReturn();
        String csrfToken =
                json(tokenResult.getResponse().getContentAsByteArray()).get("token").asText();
        mockMvc.perform(
                        post("/api/v1/auth/logout")
                                .cookie(session)
                                .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void openTeacherRegistrationIsLimitedBeforeCreatingMoreIdentities() throws Exception {
        for (int i = 0; i < 2; i++) {
            perform(
                            post("/api/v1/auth/register/teacher"),
                            CLIENT,
                            registrationBody("teacher" + i + "@example.com"),
                            obtainCsrf())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.roles[0]").value("TEACHER"));
        }
        expectRateLimited(
                perform(
                        post("/api/v1/auth/register/teacher"),
                        CLIENT,
                        registrationBody("blocked@example.com"),
                        obtainCsrf()));
        assertThat(userRepository.count()).isEqualTo(2);
        assertThat(teacherRepository.count()).isEqualTo(2);
    }

    @Test
    void inviteOnlyRegistrationStillRejectsWithTheExistingPolicyError() throws Exception {
        jdbcClient.sql("UPDATE platform_settings SET registration_mode = 'INVITE_ONLY'").update();
        perform(
                        post("/api/v1/auth/register/teacher"),
                        CLIENT,
                        registrationBody("blocked@example.com"),
                        obtainCsrf())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REGISTRATION_INVITE_REQUIRED"));
        assertThat(userRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"student", "teacher"})
    void invitationAcceptanceSharesQuotaAcrossTokensButMetadataGetIsNotLimited(String type)
            throws Exception {
        for (int i = 0; i < 2; i++) {
            accept(type, "unknown-token-" + i).andExpect(status().isNotFound());
        }
        expectRateLimited(accept(type, "another-secret-token"));
        mockMvc.perform(get("/api/v1/public/" + type + "-invitations/another-secret-token"))
                .andExpect(status().isNotFound());
        TIME.addAndGet(Duration.ofSeconds(60).toNanos());
        accept(type, "unknown-token-after-window").andExpect(status().isNotFound());
        assertThat(userRepository.count()).isZero();
    }

    @Test
    void spoofedForwardedHeadersCannotRotateBucketsButDifferentPeersAndOperationsAreIndependent()
            throws Exception {
        for (int i = 0; i < 2; i++) {
            perform(
                            post("/api/v1/auth/login")
                                    .header("X-Forwarded-For", "198.51.100." + i)
                                    .header("Forwarded", "for=198.51.100." + i)
                                    .header("X-Real-IP", "198.51.100." + i),
                            CLIENT,
                            loginBody("missing@example.com", PASSWORD),
                            obtainCsrf())
                    .andExpect(status().isUnauthorized());
        }
        expectRateLimited(
                perform(
                        post("/api/v1/auth/login").header("X-Forwarded-For", "203.0.113.99"),
                        CLIENT,
                        loginBody("different@example.com", PASSWORD),
                        obtainCsrf()));
        login("192.0.2.2", "missing@example.com", PASSWORD).andExpect(status().isUnauthorized());
        accept("student", "unknown-token").andExpect(status().isNotFound());
        accept("teacher", "unknown-token").andExpect(status().isNotFound());
        perform(
                        post("/api/v1/auth/register/teacher"),
                        CLIENT,
                        registrationBody("independent@example.com"),
                        obtainCsrf())
                .andExpect(status().isCreated());
    }

    @Test
    void csrfErrorsRemainForbiddenAndDoNotConsumeAuthenticationQuota() throws Exception {
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(
                            post("/api/v1/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(loginBody("missing@example.com", PASSWORD))
                                    .with(
                                            request -> {
                                                request.setRemoteAddr(CLIENT);
                                                return request;
                                            }))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        }
        login(CLIENT, "missing@example.com", PASSWORD).andExpect(status().isUnauthorized());
        login(CLIENT, "missing@example.com", PASSWORD).andExpect(status().isUnauthorized());
        expectRateLimited(login(CLIENT, "missing@example.com", PASSWORD));
        mockMvc.perform(post("/api/v1/auth/login"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    }

    @Test
    void openApiDocuments429OnAllProtectedOperations() throws Exception {
        var result = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var paths = json(result.getResponse().getContentAsByteArray()).get("paths");
        for (String path :
                new String[] {
                    "/api/v1/auth/login",
                    "/api/v1/auth/register/teacher",
                    "/api/v1/public/student-invitations/{token}/accept",
                    "/api/v1/public/teacher-invitations/{token}/accept"
                }) {
            assertThat(paths.get(path).get("post").get("responses").has("429")).isTrue();
        }
    }

    private void createTeacher() {
        var user =
                new UserEntity(
                        UUID.randomUUID(),
                        "teacher@example.com",
                        passwordEncoder.encode(PASSWORD),
                        UserStatus.ACTIVE);
        user.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(user);
        teacherRepository.saveAndFlush(new TeacherEntity(UUID.randomUUID(), user, "Teacher"));
    }

    private ResultActions login(String address, String email, String password) throws Exception {
        return perform(
                post("/api/v1/auth/login"), address, loginBody(email, password), obtainCsrf());
    }

    private ResultActions accept(String type, String token) throws Exception {
        return perform(
                post("/api/v1/public/" + type + "-invitations/{token}/accept", token),
                CLIENT,
                objectMapper.writeValueAsString(
                        Map.of("password", PASSWORD, "displayName", "Teacher")),
                obtainCsrf());
    }

    private ResultActions perform(
            MockHttpServletRequestBuilder request, String address, String body, Csrf csrf)
            throws Exception {
        return mockMvc.perform(
                request.cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(
                                servlet -> {
                                    servlet.setRemoteAddr(address);
                                    return servlet;
                                }));
    }

    private void expectRateLimited(ResultActions action) throws Exception {
        var result =
                action.andExpect(status().isTooManyRequests())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(header().string("Retry-After", "60"))
                        .andExpect(header().string("Cache-Control", "no-store"))
                        .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"))
                        .andExpect(
                                jsonPath("$.message")
                                        .value(
                                                "Too many authentication requests. Try again later."))
                        .andExpect(jsonPath("$.timestamp").isNotEmpty())
                        .andExpect(jsonPath("$.traceId").isNotEmpty())
                        .andExpect(jsonPath("$.details").isEmpty())
                        .andReturn();
        var error = json(result.getResponse().getContentAsByteArray());
        assertThat(error.get("traceId").asText())
                .isEqualTo(result.getResponse().getHeader("X-Trace-Id"));
        assertThat(Instant.parse(error.get("timestamp").asText())).isNotNull();
        assertThat(error.fieldNames())
                .toIterable()
                .containsExactlyInAnyOrder("code", "message", "timestamp", "traceId", "details");
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(PASSWORD, "@example.com", "secret-token");
        assertThat(result.getResponse().getCookie("TUTOR_SESSION")).isNull();
    }

    private String loginBody(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of("email", email, "password", password));
    }

    private String registrationBody(String email) throws Exception {
        return objectMapper.writeValueAsString(
                Map.of("email", email, "password", PASSWORD, "displayName", "Teacher"));
    }

    private Csrf obtainCsrf() throws Exception {
        var result =
                mockMvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
        return new Csrf(
                result.getResponse().getCookie("TUTOR_SESSION"),
                json(result.getResponse().getContentAsByteArray()).get("token").asText());
    }

    private JsonNode json(byte[] body) throws Exception {
        return objectMapper.readTree(body);
    }

    private record Csrf(Cookie cookie, String token) {}
}
