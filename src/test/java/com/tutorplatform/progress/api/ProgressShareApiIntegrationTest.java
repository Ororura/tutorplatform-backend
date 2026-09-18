package com.tutorplatform.progress.api;

import com.tutorplatform.test.PostgresIntegrationTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.student.application.invite.StudentInviteTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProgressShareApiIntegrationTest extends PostgresIntegrationTest {

    private static final String FRONTEND_BASE_URL = "https://learning.example.test";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private StudentInviteTokenService tokenService;

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_progress_share_api", "008");
        registry.add("app.student-invites.public-frontend-base-url", () -> FRONTEND_BASE_URL + "/");
    }

    @Test
    void teacherCreatesSecureSharesAndListsOnlyMetadata() throws Exception {
        Fixture fixture = fixture("create");
        Instant expiresAt = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);

        MvcResult createdResult = create(fixture, fixture.studentProgramId(), expiresAt)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.studentProgramId").value(fixture.studentProgramId().toString()))
            .andExpect(jsonPath("$.expiresAt").value(expiresAt.toString()))
            .andExpect(jsonPath("$.shareUrl").value(org.hamcrest.Matchers.startsWith(
                FRONTEND_BASE_URL + "/progress/"
            )))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.tokenHash").doesNotExist())
            .andReturn();

        JsonNode created = json(createdResult);
        UUID shareId = UUID.fromString(created.required("id").textValue());
        String rawToken = tokenFrom(created);
        String storedHash = jdbc.queryForObject(
            "select token_hash from progress_shares where id = ?", String.class, shareId
        );

        assertThat(Base64.getUrlDecoder().decode(rawToken)).hasSize(32);
        assertThat(storedHash).isEqualTo(tokenService.hash(rawToken)).hasSize(64).doesNotContain(rawToken);
        assertThat(jdbc.queryForObject(
            "select created_by_teacher_id from progress_shares where id = ?", UUID.class, shareId
        )).isEqualTo(fixture.teacherId());
        assertThat(jdbc.queryForObject(
            "select count(*) from progress_shares where token_hash = ? or token_hash like ?",
            Integer.class, rawToken, "%" + rawToken + "%"
        )).isZero();

        MvcResult second = create(fixture, fixture.studentProgramId(), null)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.expiresAt").value((Object) null))
            .andReturn();
        assertThat(tokenFrom(json(second))).isNotEqualTo(rawToken);
        assertThat(jdbc.queryForObject(
            "select count(*) from pg_indexes where tablename = 'progress_shares' and indexdef like '%UNIQUE%token_hash%'",
            Integer.class
        )).isEqualTo(1);

        MvcResult list = mockMvc.perform(get(sharesUrl(fixture)).with(user(fixture.principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$.items[0].shareUrl").doesNotExist())
            .andExpect(jsonPath("$.items[0].rawToken").doesNotExist())
            .andExpect(jsonPath("$.items[0].tokenHash").doesNotExist())
            .andReturn();
        assertThat(list.getResponse().getContentAsString()).doesNotContain(rawToken).doesNotContain(storedHash);

        mockMvc.perform(get(sharesUrl(fixture)).with(user(fixture.principal()))
                .param("studentProgramId", fixture.studentProgramId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void createValidatesOwnershipProgramExpiryAuthenticationAndCsrf() throws Exception {
        Fixture owner = fixture("owner");
        Fixture foreign = fixture("foreign");

        create(owner, foreign.studentProgramId(), Instant.now().plusSeconds(3600))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("STUDENT_PROGRAM_NOT_FOUND"));
        mockMvc.perform(post(sharesUrl(foreign)).with(user(owner.principal())).with(csrf())
                .contentType("application/json")
                .content(request(foreign.studentProgramId(), Instant.now().plusSeconds(3600))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));
        create(owner, owner.studentProgramId(), Instant.now().minusSeconds(1))
            .andExpect(status().isBadRequest());
        mockMvc.perform(post(sharesUrl(owner)).with(user(owner.principal()))
                .contentType("application/json").content(request(owner.studentProgramId(), null)))
            .andExpect(status().isForbidden());
        mockMvc.perform(post(sharesUrl(owner)).with(csrf())
                .contentType("application/json").content(request(owner.studentProgramId(), null)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void listComputesStatusesWithRevokedPrecedenceAndScopesTeacher() throws Exception {
        Fixture fixture = fixture("statuses");
        UUID active = share(fixture, "active-token", null, null);
        UUID expired = share(fixture, "expired-token", Instant.now().minusSeconds(10), null);
        UUID revoked = share(fixture, "revoked-token", Instant.now().minusSeconds(10), Instant.now());

        MvcResult result = mockMvc.perform(get(sharesUrl(fixture)).with(user(fixture.principal())))
            .andExpect(status().isOk()).andReturn();
        assertThat(statusById(json(result), active)).isEqualTo("ACTIVE");
        assertThat(statusById(json(result), expired)).isEqualTo("EXPIRED");
        assertThat(statusById(json(result), revoked)).isEqualTo("REVOKED");

        Fixture foreign = fixture("list-foreign");
        share(foreign, "foreign-token", null, null);
        mockMvc.perform(get(sharesUrl(foreign)).with(user(fixture.principal())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));
    }

    @Test
    void revokeIsIdempotentPersistentAndOwnershipScoped() throws Exception {
        Fixture owner = fixture("revoke-owner");
        Fixture foreign = fixture("revoke-foreign");
        UUID shareId = share(owner, "revoke-token", null, null);

        revoke(owner, shareId).andExpect(status().isNoContent());
        Instant revokedAt = jdbc.queryForObject(
            "select revoked_at from progress_shares where id = ?", Instant.class, shareId
        );
        assertThat(revokedAt).isNotNull();
        revoke(owner, shareId).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject(
            "select count(*) from progress_shares where id = ?", Integer.class, shareId
        )).isOne();

        revoke(foreign, shareId)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PROGRESS_SHARE_NOT_FOUND"));
        mockMvc.perform(delete(sharesUrl(owner) + "/" + shareId).with(user(owner.principal())))
            .andExpect(status().isForbidden());
    }

    @Test
    void publicTokenReturnsLiveParentSafeProgressWithoutSessionOrCsrf() throws Exception {
        Fixture fixture = fixture("public-live");
        session(fixture, 120, "initial-private-notes@example.com");
        JsonNode created = json(create(fixture, fixture.studentProgramId(), null)
            .andExpect(status().isCreated()).andReturn());
        String rawToken = tokenFrom(created);

        MvcResult first = mockMvc.perform(get("/api/v1/public/progress/{token}", rawToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalLearningMinutes").value(120))
            .andExpect(jsonPath("$.sessionsCount").value(1))
            .andExpect(jsonPath("$.studentId").doesNotExist())
            .andExpect(jsonPath("$.teacherId").doesNotExist())
            .andExpect(jsonPath("$.studentProgramId").doesNotExist())
            .andExpect(jsonPath("$.tokenHash").doesNotExist())
            .andExpect(jsonPath("$.rawToken").doesNotExist())
            .andReturn();
        assertParentSafe(first.getResponse().getContentAsString(), fixture, rawToken);

        session(fixture, 60, "new-private-notes@example.com");
        MvcResult second = mockMvc.perform(get("/api/v1/public/progress/{token}", rawToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalLearningMinutes").value(180))
            .andExpect(jsonPath("$.sessionsCount").value(2))
            .andReturn();
        assertThat(second.getResponse().getContentAsString()).doesNotContain(rawToken);
        assertThat(jdbc.queryForObject(
            "select count(*) from progress_shares where id = ?", Integer.class,
            UUID.fromString(created.required("id").textValue())
        )).isOne();
    }

    @Test
    void publicLookupNormalizesUnknownAndReturnsGoneForExpiredOrRevoked() throws Exception {
        Fixture fixture = fixture("public-state");
        share(fixture, "expired-known-token", Instant.now().minusSeconds(1), null);
        share(fixture, "revoked-known-token", null, Instant.now());

        mockMvc.perform(get("/api/v1/public/progress/random-invalid-token"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PROGRESS_SHARE_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/public/progress/expired-known-token"))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.code").value("PROGRESS_SHARE_EXPIRED"));
        mockMvc.perform(get("/api/v1/public/progress/revoked-known-token"))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.code").value("PROGRESS_SHARE_REVOKED"));
    }

    @Test
    void openApiPublishesShareOperationsAndPublicProjection() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/v1/teacher/students/{studentId}/progress/shares'].post.operationId")
                .value("createProgressShare"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/students/{studentId}/progress/shares'].get.operationId")
                .value("listProgressShares"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/students/{studentId}/progress/shares/{shareId}'].delete.operationId")
                .value("revokeProgressShare"))
            .andExpect(jsonPath("$.paths['/api/v1/public/progress/{token}'].get.operationId")
                .value("getPublicCurrentProgress"))
            .andExpect(jsonPath("$.paths['/api/v1/public/progress/{token}'].get.security").isEmpty())
            .andExpect(jsonPath("$.components.schemas.ProgressShareCreatedResponse").exists())
            .andExpect(jsonPath("$.components.schemas.ProgressShareSummaryResponse").exists())
            .andExpect(jsonPath("$.components.schemas.ProgressShareStatus.enum.length()").value(3))
            .andExpect(jsonPath("$.components.schemas.PublicCurrentProgressResponse").exists())
            .andExpect(jsonPath("$.components.schemas.PublicCurrentProgressResponse.properties.studentProgramId")
                .doesNotExist())
            .andExpect(jsonPath("$.components.schemas.PublicTopicResponse.properties.id").doesNotExist())
            .andExpect(jsonPath("$.components.schemas.ApiError").exists());
    }

    private org.springframework.test.web.servlet.ResultActions create(
        Fixture fixture, UUID studentProgramId, Instant expiresAt
    ) throws Exception {
        return mockMvc.perform(post(sharesUrl(fixture)).with(user(fixture.principal())).with(csrf())
            .contentType("application/json").content(request(studentProgramId, expiresAt)));
    }

    private org.springframework.test.web.servlet.ResultActions revoke(Fixture fixture, UUID shareId) throws Exception {
        return mockMvc.perform(delete(sharesUrl(fixture) + "/" + shareId)
            .with(user(fixture.principal())).with(csrf()));
    }

    private String request(UUID studentProgramId, Instant expiresAt) {
        var request = objectMapper.createObjectNode().put("studentProgramId", studentProgramId.toString());
        if (expiresAt != null) {
            request.put("expiresAt", expiresAt.toString());
        }
        return request.toString();
    }

    private Fixture fixture(String suffix) {
        UUID teacherUserId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID learningProgramId = UUID.randomUUID();
        UUID studentProgramId = UUID.randomUUID();
        jdbc.update("insert into users(id, email) values (?, ?)", teacherUserId,
            suffix + "-teacher-" + teacherUserId + "@example.com");
        jdbc.update("insert into teachers(id, user_id, display_name) values (?, ?, 'Teacher')",
            teacherId, teacherUserId);
        jdbc.update("insert into students(id, first_name, last_name) values (?, 'Student', 'PrivateSurname')",
            studentId);
        jdbc.update("insert into teacher_student_links(teacher_id, student_id) values (?, ?)",
            teacherId, studentId);
        jdbc.update("insert into subjects(id, owner_teacher_id, name) values (?, ?, 'Subject')",
            subjectId, teacherId);
        jdbc.update("insert into learning_programs(id, teacher_id, subject_id, title, status) values (?, ?, ?, 'Program', 'ACTIVE')",
            learningProgramId, teacherId, subjectId);
        jdbc.update("insert into student_programs(id, student_id, learning_program_id, assigned_by_teacher_id) values (?, ?, ?, ?)",
            studentProgramId, studentId, learningProgramId, teacherId);
        return new Fixture(teacherUserId, teacherId, studentId, studentProgramId);
    }

    private UUID share(Fixture fixture, String rawToken, Instant expiresAt, Instant revokedAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            insert into progress_shares(
                id, student_program_id, created_by_teacher_id, token_hash, expires_at, revoked_at
            ) values (?, ?, ?, ?, ?, ?)
            """, id, fixture.studentProgramId(), fixture.teacherId(), tokenService.hash(rawToken),
            timestamp(expiresAt), timestamp(revokedAt));
        return id;
    }

    private Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private void session(Fixture fixture, int minutes, String privateNotes) {
        jdbc.update("""
            insert into lesson_sessions(
                id, student_program_id, teacher_id, started_at, duration_minutes, attendance_status, private_notes
            ) values (?, ?, ?, now(), ?, 'ATTENDED', ?)
            """, UUID.randomUUID(), fixture.studentProgramId(), fixture.teacherId(), minutes, privateNotes);
    }

    private String sharesUrl(Fixture fixture) {
        return "/api/v1/teacher/students/" + fixture.studentId() + "/progress/shares";
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private String tokenFrom(JsonNode response) {
        String url = response.required("shareUrl").textValue();
        return url.substring(url.lastIndexOf('/') + 1);
    }

    private String statusById(JsonNode response, UUID id) {
        for (JsonNode item : response.required("items")) {
            if (item.required("id").textValue().equals(id.toString())) {
                return item.required("status").textValue();
            }
        }
        throw new AssertionError("Share not found: " + id);
    }

    private void assertParentSafe(String body, Fixture fixture, String rawToken) {
        assertThat(body)
            .doesNotContain(fixture.studentId().toString())
            .doesNotContain(fixture.teacherId().toString())
            .doesNotContain(fixture.studentProgramId().toString())
            .doesNotContain("@example.com")
            .doesNotContain("private-notes")
            .doesNotContain("textAnswer")
            .doesNotContain("sourceCode")
            .doesNotContain("hidden")
            .doesNotContain("tokenHash")
            .doesNotContain(rawToken);
    }

    private record Fixture(UUID teacherUserId, UUID teacherId, UUID studentId, UUID studentProgramId) {
        private AuthenticatedUser principal() {
            return new AuthenticatedUser(
                teacherUserId, teacherUserId + "@example.com", "hash", true,
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
            );
        }
    }
}
