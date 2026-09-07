package com.tutorplatform.student.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.student.api.requrest.AcceptStudentInviteRequest;
import com.tutorplatform.student.application.StudentInviteTokenService;
import com.tutorplatform.student.domain.StudentStatus;
import com.tutorplatform.student.domain.StudentEntity;
import com.tutorplatform.student.domain.StudentRepository;
import com.tutorplatform.student.infrastructure.persistence.StudentInviteEntity;
import com.tutorplatform.student.infrastructure.persistence.StudentInviteRepository;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkEntity;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkRepository;
import com.tutorplatform.user.domain.UserRole;
import com.tutorplatform.user.domain.UserStatus;
import com.tutorplatform.user.domain.TeacherEntity;
import com.tutorplatform.user.domain.TeacherRepository;
import com.tutorplatform.user.domain.UserEntity;
import com.tutorplatform.user.domain.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PublicStudentInvitationApiIntegrationTest {

    private static final String PASSWORD = "correct horse battery staple";

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

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
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StudentInviteTokenService tokenService;

    @Autowired
    private StudentInviteRepository studentInviteRepository;

    @Autowired
    private TeacherStudentLinkRepository teacherStudentLinkRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private UserRepository userRepository;

    private TeacherEntity teacher;
    private StudentEntity student;

    @BeforeEach
    void cleanData() {
        studentInviteRepository.deleteAll();
        teacherStudentLinkRepository.deleteAll();
        studentRepository.deleteAll();
        teacherRepository.deleteAll();
        userRepository.deleteAll();

        UserEntity teacherUser = new UserEntity(
                UUID.randomUUID(), "teacher@example.com", "password-hash", UserStatus.ACTIVE
        );
        teacherUser.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(teacherUser);
        teacher = teacherRepository.saveAndFlush(new TeacherEntity(UUID.randomUUID(), teacherUser, "Егор"));
        student = studentRepository.saveAndFlush(new StudentEntity(
                UUID.randomUUID(), "Андрей", "Иванов", StudentStatus.ACTIVE
        ));
        teacherStudentLinkRepository.saveAndFlush(new TeacherStudentLinkEntity(teacher, student));
    }

    @Test
    void getReturnsOnlySafeInvitationMetadata() throws Exception {
        String rawToken = "active-public-token";
        persistInvite(rawToken, "student@example.com", Instant.now().plusSeconds(3600));

        MvcResult result = mockMvc.perform(get("/api/v1/public/student-invitations/{token}", rawToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.student.firstName").value("Андрей"))
                .andExpect(jsonPath("$.student.lastName").value("Иванов"))
                .andExpect(jsonPath("$.teacher.displayName").value("Егор"))
                .andExpect(jsonPath("$.email").value("student@example.com"))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andReturn();

        assertThat(json(result).fieldNames()).toIterable()
                .containsExactlyInAnyOrder("student", "teacher", "email", "expiresAt");
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(student.getId().toString(), teacher.getId().toString(), "tokenHash", "token_hash");
    }

    @Test
    void invalidTokenIsNotFoundAndKnownUnavailableStatesAreGone() throws Exception {
        mockMvc.perform(get("/api/v1/public/student-invitations/{token}", "random-invalid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_INVITE_NOT_FOUND"));
        accept("random-invalid-token", obtainCsrf())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_INVITE_NOT_FOUND"));

        StudentInviteEntity expired = persistInvite(
                "expired-token", "expired@example.com", Instant.now().plusSeconds(30)
        );
        jdbcTemplate.update(
                "update student_invites set created_at = now() - interval '2 minutes', expires_at = now() - interval '1 minute' where id = ?",
                expired.getId()
        );
        assertGoneForGetAndAccept("expired-token", "STUDENT_INVITE_EXPIRED");

        StudentInviteEntity revoked = persistInvite(
                "revoked-token", "revoked@example.com", Instant.now().plusSeconds(3600)
        );
        jdbcTemplate.update("update student_invites set revoked_at = created_at where id = ?", revoked.getId());
        assertGoneForGetAndAccept("revoked-token", "STUDENT_INVITE_REVOKED");

        StudentInviteEntity accepted = persistInvite(
                "accepted-token", "accepted@example.com", Instant.now().plusSeconds(3600)
        );
        jdbcTemplate.update("update student_invites set accepted_at = created_at where id = ?", accepted.getId());
        assertGoneForGetAndAccept("accepted-token", "STUDENT_INVITE_ALREADY_ACCEPTED");
    }

    @Test
    void acceptCreatesStudentIdentityAndAuthenticatedSessionAtomically() throws Exception {
        String rawToken = "successful-accept-token";
        StudentInviteEntity invite = persistInvite(
                rawToken, "student@example.com", Instant.now().plusSeconds(3600)
        );
        CsrfExchange csrf = obtainCsrf();

        MvcResult result = accept(rawToken, csrf)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student@example.com"))
                .andExpect(jsonPath("$.displayName").value("Андрей Иванов"))
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.roles[0]").value("STUDENT"))
                .andReturn();

        UserEntity user = userRepository.findByEmail("STUDENT@example.com").orElseThrow();
        assertThat(user.getRoles()).containsExactly(UserRole.STUDENT);
        assertThat(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).isTrue();
        assertThat(studentRepository.findById(student.getId()).orElseThrow().getUserId()).isEqualTo(user.getId());
        assertThat(studentInviteRepository.findById(invite.getId()).orElseThrow().getAcceptedAt()).isNotNull();

        Cookie authenticatedSession = sessionCookieFrom(result, csrf.sessionCookie());
        mockMvc.perform(get("/api/v1/auth/me").cookie(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.displayName").value("Андрей Иванов"))
                .andExpect(jsonPath("$.roles[0]").value("STUDENT"));

        CsrfExchange repeatedCsrf = obtainCsrf();
        accept(rawToken, repeatedCsrf)
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("STUDENT_INVITE_ALREADY_ACCEPTED"));
        assertThat(userRepository.count()).isEqualTo(2);
    }

    @Test
    void acceptRejectsUnavailableEmailWithoutPartialChanges() throws Exception {
        String rawToken = "email-conflict-token";
        StudentInviteEntity invite = persistInvite(
                rawToken, "student@example.com", Instant.now().plusSeconds(3600)
        );
        userRepository.saveAndFlush(new UserEntity(
                UUID.randomUUID(), "STUDENT@example.com", "password-hash", UserStatus.ACTIVE
        ));

        accept(rawToken, obtainCsrf())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STUDENT_INVITE_EMAIL_CONFLICT"));

        assertThat(studentRepository.findById(student.getId()).orElseThrow().getUserId()).isNull();
        assertThat(studentInviteRepository.findById(invite.getId()).orElseThrow().getAcceptedAt()).isNull();
    }

    @Test
    void twoConcurrentAcceptsCreateExactlyOneUser() throws Exception {
        String rawToken = "concurrent-accept-token";
        persistInvite(rawToken, "race@example.com", Instant.now().plusSeconds(3600));
        CsrfExchange firstCsrf = obtainCsrf();
        CsrfExchange secondCsrf = obtainCsrf();
        CyclicBarrier start = new CyclicBarrier(2);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                start.await();
                return accept(rawToken, firstCsrf).andReturn();
            });
            var second = executor.submit(() -> {
                start.await();
                return accept(rawToken, secondCsrf).andReturn();
            });

            List<Integer> statuses = List.of(
                    first.get().getResponse().getStatus(),
                    second.get().getResponse().getStatus()
            );
            assertThat(statuses).containsExactlyInAnyOrder(200, 410);
        }

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from users where email = cast(? as citext)", Integer.class, "race@example.com"
        )).isOne();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from user_roles roles join users users on users.id = roles.user_id where users.email = cast(? as citext) and roles.role = 'STUDENT'",
                Integer.class,
                "race@example.com"
        )).isOne();
        assertThat(studentInviteRepository.findByTokenHash(tokenService.hash(rawToken)).orElseThrow().getAcceptedAt())
                .isNotNull();
    }

    @Test
    void acceptRequiresCsrfAndOpenApiPublishesContract() throws Exception {
        String rawToken = "csrf-token";
        persistInvite(rawToken, "student@example.com", Instant.now().plusSeconds(3600));

        mockMvc.perform(post("/api/v1/public/student-invitations/{token}/accept", rawToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/public/student-invitations/{token}'].get.operationId")
                        .value("getPublicStudentInvitation"))
                .andExpect(jsonPath("$.paths['/api/v1/public/student-invitations/{token}/accept'].post.operationId")
                        .value("acceptStudentInvitation"))
                .andExpect(jsonPath("$.components.schemas.AcceptStudentInviteRequest.properties.password.minLength")
                        .value(10));
    }

    private StudentInviteEntity persistInvite(String rawToken, String email, Instant expiresAt) {
        return studentInviteRepository.saveAndFlush(new StudentInviteEntity(
                UUID.randomUUID(), student, teacher, email, tokenService.hash(rawToken), expiresAt
        ));
    }

    private void assertGoneForGetAndAccept(String rawToken, String code) throws Exception {
        mockMvc.perform(get("/api/v1/public/student-invitations/{token}", rawToken))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value(code));
        accept(rawToken, obtainCsrf())
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value(code));
    }

    private org.springframework.test.web.servlet.ResultActions accept(String rawToken, CsrfExchange csrf)
            throws Exception {
        return mockMvc.perform(post("/api/v1/public/student-invitations/{token}/accept", rawToken)
                .cookie(csrf.sessionCookie())
                .header(csrf.headerName(), csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordJson()));
    }

    private CsrfExchange obtainCsrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = json(result);
        return new CsrfExchange(
                body.required("headerName").textValue(),
                body.required("token").textValue(),
                sessionCookieFrom(result, null)
        );
    }

    private Cookie sessionCookieFrom(MvcResult result, Cookie fallback) {
        Cookie cookie = result.getResponse().getCookie("TUTOR_SESSION");
        if (cookie != null) {
            return cookie;
        }
        assertThat(fallback).isNotNull();
        return fallback;
    }

    private String passwordJson() throws Exception {
        return objectMapper.writeValueAsString(new AcceptStudentInviteRequest(PASSWORD));
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private record CsrfExchange(String headerName, String token, Cookie sessionCookie) {
    }
}
