package com.tutorplatform.report.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.student.application.StudentInviteTokenService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class ReportShareApiIntegrationTest {

    private static final String FRONTEND_BASE_URL = "https://learning.example.test";
    private static final Instant PERIOD_START = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant PERIOD_END = Instant.parse("2026-01-31T10:00:00Z");

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private StudentInviteTokenService tokenService;

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.target", () -> "008");
        registry.add("app.student-invites.public-frontend-base-url", () -> FRONTEND_BASE_URL + "/");
    }

    @Test
    void createsPublishedReportShareWithHashedOneTimeTokenAndValidatesInput() throws Exception {
        Fixture owner = fixture("create-owner");
        Fixture foreign = fixture("create-foreign");
        UUID published = report(owner, "PUBLISHED");
        UUID draft = report(owner, "DRAFT");
        UUID foreignReport = report(foreign, "PUBLISHED");
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);

        MvcResult result = create(owner, published, expiresAt)
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith(
                sharesUrl(published) + "/"
            )))
            .andExpect(jsonPath("$.reportId").value(published.toString()))
            .andExpect(jsonPath("$.expiresAt").value(expiresAt.toString()))
            .andExpect(jsonPath("$.shareUrl").value(org.hamcrest.Matchers.startsWith(
                FRONTEND_BASE_URL + "/reports/"
            )))
            .andExpect(jsonPath("$.tokenHash").doesNotExist())
            .andReturn();

        JsonNode response = json(result);
        UUID shareId = UUID.fromString(response.required("id").asText());
        String rawToken = tokenFrom(response);
        String storedHash = jdbc.queryForObject(
            "select token_hash from report_shares where id = ?", String.class, shareId
        );
        assertThat(Base64.getUrlDecoder().decode(rawToken)).hasSize(32);
        assertThat(storedHash).isEqualTo(tokenService.hash(rawToken)).hasSize(64).doesNotContain(rawToken);
        assertThat(jdbc.queryForObject(
            "select count(*) from report_shares where token_hash = ? or token_hash like ?",
            Integer.class, rawToken, "%" + rawToken + "%"
        )).isZero();

        create(owner, published, null).andExpect(status().isCreated())
            .andExpect(jsonPath("$.expiresAt").value((Object) null));
        create(owner, published, Instant.now().minusSeconds(1)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("REPORT_SHARE_EXPIRATION_INVALID"));
        create(owner, draft, null).andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("REPORT_SHARE_NOT_ALLOWED"));
        create(owner, foreignReport, null).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PROGRESS_REPORT_NOT_FOUND"));
    }

    @Test
    void listsOnlyOwnedMetadataWithDerivedStatuses() throws Exception {
        Fixture owner = fixture("list-owner");
        Fixture foreign = fixture("list-foreign");
        UUID reportId = report(owner, "PUBLISHED");
        UUID foreignReport = report(foreign, "PUBLISHED");
        UUID active = share(owner, reportId, "active-token", null, null);
        UUID expired = share(owner, reportId, "expired-token", Instant.now().minusSeconds(10), null);
        UUID revoked = share(owner, reportId, "revoked-token", null, Instant.now());
        share(foreign, foreignReport, "foreign-token", null, null);

        MvcResult result = mockMvc.perform(get(sharesUrl(reportId)).with(user(owner.principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(3))
            .andReturn();
        JsonNode body = json(result);
        assertThat(statusById(body, active)).isEqualTo("ACTIVE");
        assertThat(statusById(body, expired)).isEqualTo("EXPIRED");
        assertThat(statusById(body, revoked)).isEqualTo("REVOKED");
        assertThat(result.getResponse().getContentAsString())
            .doesNotContain("active-token", "tokenHash", "shareUrl", foreignReport.toString());

        mockMvc.perform(get(sharesUrl(reportId)).with(user(foreign.principal())))
            .andExpect(status().isNotFound());
    }

    @Test
    void revokeIsIdempotentPersistentOwnershipScopedAndDisablesPublicAccess() throws Exception {
        Fixture owner = fixture("revoke-owner");
        Fixture foreign = fixture("revoke-foreign");
        UUID reportId = report(owner, "PUBLISHED");
        UUID foreignReport = report(foreign, "PUBLISHED");
        UUID shareId = share(owner, reportId, "revoke-token", null, null);

        revoke(owner, reportId, shareId).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject(
            "select revoked_at from report_shares where id = ?", Instant.class, shareId
        )).isNotNull();
        revoke(owner, reportId, shareId).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject(
            "select count(*) from report_shares where id = ?", Integer.class, shareId
        )).isOne();
        revoke(foreign, foreignReport, shareId).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/public/reports/revoke-token"))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.code").value("REPORT_SHARE_REVOKED"));
    }

    @Test
    void publicReadUsesPersistedSnapshotAndReturnsNoInternalOrSensitiveData() throws Exception {
        Fixture fixture = fixture("public-history");
        UUID reportId = report(fixture, "PUBLISHED");
        JsonNode created = json(create(fixture, reportId, null)
            .andExpect(status().isCreated()).andReturn());
        String rawToken = tokenFrom(created);

        MvcResult first = mockMvc.perform(get("/api/v1/public/reports/{token}", rawToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.periodStartedAt").value(PERIOD_START.toString()))
            .andExpect(jsonPath("$.learningMinutes").value(90))
            .andExpect(jsonPath("$.snapshot.metrics.learningMinutes").value(90))
            .andExpect(jsonPath("$.snapshot.topics.completed[0].title").value("Historical title"))
            .andExpect(jsonPath("$.teacherSummary").value("Historical summary"))
            .andExpect(jsonPath("$.nextPeriodPlan").value("Historical plan"))
            .andExpect(jsonPath("$.studentId").doesNotExist())
            .andExpect(jsonPath("$.studentProgramId").doesNotExist())
            .andExpect(jsonPath("$.generatedByTeacherId").doesNotExist())
            .andExpect(jsonPath("$.version").doesNotExist())
            .andExpect(jsonPath("$.snapshotSchemaVersion").doesNotExist())
            .andExpect(jsonPath("$.snapshot.topics.completed[0].id").doesNotExist())
            .andReturn();

        UUID sessionId = UUID.randomUUID();
        jdbc.update("insert into lesson_sessions(id, student_program_id, teacher_id, started_at, duration_minutes, attendance_status, private_notes) values (?, ?, ?, now(), 120, 'ATTENDED', 'privateNotes-leak@example.com')",
            sessionId, fixture.studentProgramId(), fixture.teacherId());
        jdbc.update("insert into teacher_assessments(id, lesson_session_id, understanding_score, public_comment) values (?, ?, 5, 'changed assessment')",
            UUID.randomUUID(), sessionId);
        jdbc.update("update topics set title = 'Renamed source topic' where id = ?", fixture.topicId());

        MvcResult second = mockMvc.perform(get("/api/v1/public/reports/{token}", rawToken))
            .andExpect(status().isOk()).andReturn();
        assertThat(second.getResponse().getContentAsString())
            .isEqualTo(first.getResponse().getContentAsString())
            .doesNotContain(
                fixture.userId().toString(), fixture.teacherId().toString(),
                fixture.studentId().toString(), fixture.studentProgramId().toString(),
                fixture.topicId().toString(), rawToken, "tokenHash", "privateNotes",
                "@example.com", "textAnswer", "sourceCode", "expectedOutput"
            );
    }

    @Test
    void publicLookupUsesEstablishedNotFoundGoneAndPublishedOnlySemantics() throws Exception {
        Fixture fixture = fixture("public-state");
        UUID published = report(fixture, "PUBLISHED");
        UUID draft = report(fixture, "DRAFT");
        share(fixture, published, "expired-token", Instant.now().minusSeconds(1), null);
        share(fixture, published, "revoked-token", null, Instant.now());
        share(fixture, draft, "draft-token", null, null);

        mockMvc.perform(get("/api/v1/public/reports/random-token"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("REPORT_SHARE_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/public/reports/expired-token"))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.code").value("REPORT_SHARE_EXPIRED"));
        mockMvc.perform(get("/api/v1/public/reports/revoked-token"))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.code").value("REPORT_SHARE_REVOKED"));
        mockMvc.perform(get("/api/v1/public/reports/draft-token"))
            .andExpect(status().isNotFound());
    }

    @Test
    void publicPdfUsesTheSameShareValidationAndLeaksNoPrivateSourceData() throws Exception {
        Fixture fixture = fixture("public-pdf");
        UUID published = report(fixture, "PUBLISHED");
        UUID draft = report(fixture, "DRAFT");
        UUID archived = report(fixture, "ARCHIVED");
        share(fixture, published, "pdf-active-token", null, null);
        share(fixture, published, "pdf-expired-token", Instant.now().minusSeconds(1), null);
        share(fixture, published, "pdf-revoked-token", null, Instant.now());
        share(fixture, draft, "pdf-draft-token", null, null);
        share(fixture, archived, "pdf-archived-token", null, null);

        jdbc.update("update users set email = 'do-not-leak@example.com' where id = ?", fixture.userId());
        jdbc.update("insert into lesson_sessions(id, student_program_id, teacher_id, started_at, duration_minutes, attendance_status, private_notes) values (?, ?, ?, now(), 120, 'ATTENDED', ?)",
            UUID.randomUUID(), fixture.studentProgramId(), fixture.teacherId(),
            "DO_NOT_LEAK_PRIVATE_NOTES");
        UUID taskId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        jdbc.update("insert into tasks(id, teacher_id, subject_id, title, description_markdown, task_type, status) values (?, ?, ?, 'Private task', '', 'CODE', 'ACTIVE')",
            taskId, fixture.teacherId(), fixture.subjectId());
        jdbc.update("insert into task_test_cases(id, task_id, expected_output, hidden, position) values (?, ?, 'DO_NOT_LEAK_HIDDEN_TEST', true, 0)",
            UUID.randomUUID(), taskId);
        jdbc.update("insert into submissions(id, student_id, student_program_id, task_id, attempt_no, status, text_answer) values (?, ?, ?, ?, 1, 'PASSED', 'DO_NOT_LEAK_TEXT_ANSWER')",
            submissionId, fixture.studentId(), fixture.studentProgramId(), taskId);
        jdbc.update("insert into code_submissions(submission_id, source_code, execution_status) values (?, 'DO_NOT_LEAK_SOURCE', 'PASSED')",
            submissionId);

        String url = "/api/v1/public/reports/pdf-active-token/pdf";
        MvcResult first = mockMvc.perform(get(url))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(header().string("Content-Disposition",
                "attachment; filename=\"progress-report-" + published + ".pdf\""))
            .andReturn();
        String firstText = pdfText(first);
        assertThat(firstText)
            .contains("Отчёт о прогрессе", "Historical title", "Historical summary", "Historical plan")
            .doesNotContain(
                "DO_NOT_LEAK_PRIVATE_NOTES", "DO_NOT_LEAK_TEXT_ANSWER", "DO_NOT_LEAK_SOURCE",
                "DO_NOT_LEAK_HIDDEN_TEST", "do-not-leak@example.com", fixture.topicId().toString(),
                fixture.teacherId().toString(), "pdf-active-token"
            );

        jdbc.update("update topics set title = 'Changed after publication' where id = ?", fixture.topicId());
        MvcResult second = mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        assertThat(pdfText(second)).isEqualTo(firstText).doesNotContain("Changed after publication");

        mockMvc.perform(get("/api/v1/public/reports/missing-token/pdf"))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/public/reports/pdf-expired-token/pdf"))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.code").value("REPORT_SHARE_EXPIRED"));
        mockMvc.perform(get("/api/v1/public/reports/pdf-revoked-token/pdf"))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.code").value("REPORT_SHARE_REVOKED"));
        mockMvc.perform(get("/api/v1/public/reports/pdf-draft-token/pdf"))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/public/reports/pdf-archived-token/pdf"))
            .andExpect(status().isNotFound());
    }

    @Test
    void teacherSecurityCsrfAndOpenApiContractAreExact() throws Exception {
        Fixture fixture = fixture("security");
        UUID reportId = report(fixture, "PUBLISHED");
        UUID shareId = share(fixture, reportId, "security-token", null, null);

        mockMvc.perform(get(sharesUrl(reportId))).andExpect(status().isUnauthorized());
        mockMvc.perform(post(sharesUrl(reportId)).with(user(fixture.principal()))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
        mockMvc.perform(delete(sharesUrl(reportId) + "/" + shareId)
                .with(user(fixture.principal())))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/public/reports/security-token"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/v1/teacher/reports/{reportId}/shares'].post.operationId")
                .value("createReportShare"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/reports/{reportId}/shares'].get.operationId")
                .value("listReportShares"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/reports/{reportId}/shares/{shareId}'].delete.operationId")
                .value("revokeReportShare"))
            .andExpect(jsonPath("$.paths['/api/v1/public/reports/{token}'].get.operationId")
                .value("getPublicProgressReport"))
            .andExpect(jsonPath("$.paths['/api/v1/public/reports/{token}/pdf'].get.operationId")
                .value("downloadPublicProgressReportPdf"))
            .andExpect(jsonPath("$.paths['/api/v1/public/reports/{token}/pdf'].get.responses['200'].content['application/pdf'].schema.format")
                .value("binary"))
            .andExpect(jsonPath("$.paths['/api/v1/public/reports/{token}/pdf'].get.security").isEmpty())
            .andExpect(jsonPath("$.paths['/api/v1/public/reports/{token}'].get.security").isEmpty())
            .andExpect(jsonPath("$.components.schemas.ReportShareCreatedResponse").exists())
            .andExpect(jsonPath("$.components.schemas.ReportShareSummaryResponse").exists())
            .andExpect(jsonPath("$.components.schemas.ReportShareStatus.enum.length()").value(3))
            .andExpect(jsonPath("$.components.schemas.PublicProgressReportResponse").exists());
    }

    private org.springframework.test.web.servlet.ResultActions create(
        Fixture fixture, UUID reportId, Instant expiresAt
    ) throws Exception {
        var body = objectMapper.createObjectNode();
        if (expiresAt != null) {
            body.put("expiresAt", expiresAt.toString());
        }
        return mockMvc.perform(post(sharesUrl(reportId))
            .with(user(fixture.principal())).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(body.toString()));
    }

    private org.springframework.test.web.servlet.ResultActions revoke(
        Fixture fixture, UUID reportId, UUID shareId
    ) throws Exception {
        return mockMvc.perform(delete(sharesUrl(reportId) + "/" + shareId)
            .with(user(fixture.principal())).with(csrf()));
    }

    private Fixture fixture(String suffix) {
        UUID userId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        UUID studentProgramId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        jdbc.update("insert into users(id, email) values (?, ?)", userId,
            suffix + "-" + userId + "@example.com");
        jdbc.update("insert into teachers(id, user_id, display_name) values (?, ?, 'Teacher')",
            teacherId, userId);
        jdbc.update("insert into students(id, first_name, last_name) values (?, 'Student', 'PrivateSurname')",
            studentId);
        jdbc.update("insert into teacher_student_links(teacher_id, student_id) values (?, ?)",
            teacherId, studentId);
        jdbc.update("insert into subjects(id, owner_teacher_id, name) values (?, ?, 'Subject')",
            subjectId, teacherId);
        jdbc.update("insert into learning_programs(id, teacher_id, subject_id, title, status) values (?, ?, ?, 'Program', 'ACTIVE')",
            programId, teacherId, subjectId);
        jdbc.update("insert into student_programs(id, student_id, learning_program_id, assigned_by_teacher_id) values (?, ?, ?, ?)",
            studentProgramId, studentId, programId, teacherId);
        jdbc.update("insert into modules(id, learning_program_id, title, position) values (?, ?, 'Module', 0)",
            moduleId, programId);
        jdbc.update("insert into topics(id, module_id, title, position, status) values (?, ?, 'Historical title', 0, 'ACTIVE')",
            topicId, moduleId);
        return new Fixture(userId, teacherId, studentId, subjectId, studentProgramId, topicId);
    }

    private UUID report(Fixture fixture, String status) {
        UUID id = UUID.randomUUID();
        Instant publishedAt = status.equals("PUBLISHED") ? Instant.parse("2026-02-01T10:00:00Z") : null;
        String snapshot = """
            {"metrics":{"learningMinutes":90,"sessionsCount":1,"attendanceRate":1.0,
            "homeworkAssigned":2,"homeworkCompleted":1,"practiceAssigned":3,"practiceCompleted":2},
            "assessment":{"understandingAverage":4.0,"independenceAverage":3.0,
            "practiceAverage":4.5,"homeworkAverage":3.5},
            "topics":{"completed":[{"id":"%s","title":"Historical title"}],"inProgress":[]},
            "skills":[]}
            """.formatted(fixture.topicId());
        jdbc.update("""
            insert into progress_reports(
                id, student_program_id, generated_by_teacher_id, status,
                period_started_at, period_ended_at, learning_minutes,
                snapshot_schema_version, snapshot_json, teacher_summary, next_period_plan, published_at
            ) values (?, ?, ?, ?, ?, ?, 90, 1, cast(? as jsonb),
                'Historical summary', 'Historical plan', ?)
            """, id, fixture.studentProgramId(), fixture.teacherId(), status,
            Timestamp.from(PERIOD_START), Timestamp.from(PERIOD_END), snapshot,
            publishedAt == null ? null : Timestamp.from(publishedAt));
        return id;
    }

    private UUID share(
        Fixture fixture, UUID reportId, String rawToken, Instant expiresAt, Instant revokedAt
    ) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            insert into report_shares(
                id, report_id, created_by_teacher_id, token_hash, expires_at, revoked_at
            ) values (?, ?, ?, ?, ?, ?)
            """, id, reportId, fixture.teacherId(), tokenService.hash(rawToken),
            timestamp(expiresAt), timestamp(revokedAt));
        return id;
    }

    private Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private String sharesUrl(UUID reportId) {
        return "/api/v1/teacher/reports/" + reportId + "/shares";
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private String pdfText(MvcResult result) throws Exception {
        try (var document = Loader.loadPDF(result.getResponse().getContentAsByteArray())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String tokenFrom(JsonNode response) {
        String url = response.required("shareUrl").asText();
        return url.substring(url.lastIndexOf('/') + 1);
    }

    private String statusById(JsonNode response, UUID id) {
        for (JsonNode item : response.required("items")) {
            if (item.required("id").asText().equals(id.toString())) {
                return item.required("status").asText();
            }
        }
        throw new AssertionError("Share not found: " + id);
    }

    private record Fixture(
        UUID userId,
        UUID teacherId,
        UUID studentId,
        UUID subjectId,
        UUID studentProgramId,
        UUID topicId
    ) {
        private AuthenticatedUser principal() {
            return new AuthenticatedUser(
                userId, userId + "@example.com", "hash", true,
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
            );
        }
    }
}
