package com.tutorplatform.report.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProgressReportApiIntegrationTest {

    private static final String REPORTS_URL = "/api/v1/teacher/reports";
    private static final Instant START = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant END = Instant.parse("2026-01-01T12:00:00Z");
    private static final String SNAPSHOT = """
        {"metrics":{"learningMinutes":10,"sessionsCount":0,"attendanceRate":0.0,
        "homeworkAssigned":0,"homeworkCompleted":0,"practiceAssigned":0,"practiceCompleted":0},
        "assessment":{"understandingAverage":null,"independenceAverage":null,
        "practiceAverage":null,"homeworkAverage":null},
        "topics":{"completed":[],"inProgress":[]},"skills":[]}
        """;

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.target", () -> "008");
    }

    @Test
    void createsDraftFromOwnedCompletedPeriodUsingOnlyServerDerivedFields() throws Exception {
        Fixture fixture = fixture("create");
        UUID periodId = period(fixture, "COMPLETED", fixture.studentProgramId());

        MvcResult result = mockMvc.perform(post(REPORTS_URL)
                .with(user(fixture.teacherPrincipal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"studentProgramId":"%s","learningPeriodId":"%s",
                     "status":"PUBLISHED","learningMinutes":999,"snapshot":{"bad":true}}
                    """.formatted(fixture.studentProgramId(), periodId)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith(REPORTS_URL + "/")))
            .andExpect(jsonPath("$.studentProgramId").value(fixture.studentProgramId().toString()))
            .andExpect(jsonPath("$.learningPeriodId").value(periodId.toString()))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.learningMinutes").value(10))
            .andExpect(jsonPath("$.snapshot.metrics.learningMinutes").value(10))
            .andExpect(jsonPath("$.snapshotSchemaVersion").value(1))
            .andReturn();

        UUID reportId = UUID.fromString(json(result).get("id").asText());
        assertThat(jdbc.queryForObject(
            "select generated_by_teacher_id from progress_reports where id = ?",
            UUID.class, reportId
        )).isEqualTo(fixture.teacherId());
    }

    @Test
    void createRejectsActiveDuplicateForeignAndMismatchedPeriodsWithoutPartialRows() throws Exception {
        Fixture owner = fixture("create-errors-owner");
        Fixture foreign = fixture("create-errors-foreign");
        UUID active = period(owner, "ACTIVE", owner.studentProgramId());
        performCreate(owner, owner.studentProgramId(), active)
            .andExpect(status().isConflict());
        jdbc.update("delete from learning_periods where id = ?", active);

        UUID completed = period(owner, "COMPLETED", owner.studentProgramId());
        performCreate(owner, owner.studentProgramId(), completed)
            .andExpect(status().isCreated());
        performCreate(owner, owner.studentProgramId(), completed)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PROGRESS_REPORT_ALREADY_EXISTS"));

        UUID foreignPeriod = period(foreign, "COMPLETED", foreign.studentProgramId());
        performCreate(owner, foreign.studentProgramId(), foreignPeriod)
            .andExpect(status().isNotFound());
        performCreate(owner, owner.studentProgramId(), foreignPeriod)
            .andExpect(status().isConflict());

        assertThat(jdbc.queryForObject(
            "select count(*) from progress_reports where student_program_id = ?",
            Integer.class, owner.studentProgramId()
        )).isEqualTo(1);
    }

    @Test
    void listIsOwnershipScopedFilteredPaginatedSortedAndOmitsSnapshot() throws Exception {
        Fixture owner = fixture("list-owner");
        Fixture foreign = fixture("list-foreign");
        UUID older = report(owner, "DRAFT", START, "First", "Plan");
        UUID newer = report(owner, "PUBLISHED", END, "Second", "Plan");
        report(foreign, "DRAFT", END.plusSeconds(60), "Foreign", "Foreign");

        mockMvc.perform(get(REPORTS_URL).with(user(owner.teacherPrincipal()))
                .param("page", "0").param("size", "1").param("sort", "createdAt,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].id").value(older.toString()))
            .andExpect(jsonPath("$.items[0].snapshot").doesNotExist())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get(REPORTS_URL).with(user(owner.teacherPrincipal()))
                .param("status", "PUBLISHED")
                .param("studentProgramId", owner.studentProgramId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].id").value(newer.toString()));

        mockMvc.perform(get(REPORTS_URL).with(user(owner.teacherPrincipal()))
                .param("sort", "snapshotJson,asc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details[0].field").value("sort"));
    }

    @Test
    void detailReturnsPersistedHistoricalSnapshotAndHidesForeignReport() throws Exception {
        Fixture owner = fixture("detail-owner");
        Fixture foreign = fixture("detail-foreign");
        UUID reportId = report(owner, "PUBLISHED", START, "Summary", "Plan");

        mockMvc.perform(get(REPORTS_URL + "/" + reportId).with(user(owner.teacherPrincipal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.snapshot.metrics.learningMinutes").value(10))
            .andExpect(jsonPath("$.teacherSummary").value("Summary"))
            .andExpect(jsonPath("$.nextPeriodPlan").value("Plan"));

        jdbc.update("insert into lesson_sessions(id, student_program_id, teacher_id, started_at, duration_minutes, attendance_status) values (?, ?, ?, ?, 500, 'ATTENDED')",
            UUID.randomUUID(), owner.studentProgramId(), owner.teacherId(), Timestamp.from(END));
        mockMvc.perform(get(REPORTS_URL + "/" + reportId).with(user(owner.teacherPrincipal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.learningMinutes").value(10))
            .andExpect(jsonPath("$.snapshot.metrics.learningMinutes").value(10));

        mockMvc.perform(get(REPORTS_URL + "/" + reportId).with(user(foreign.teacherPrincipal())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PROGRESS_REPORT_NOT_FOUND"));
    }

    @Test
    void teacherDownloadsOnlyOwnedPublishedHistoricalPdfWithoutCsrf() throws Exception {
        Fixture owner = fixture("pdf-owner");
        Fixture foreign = fixture("pdf-foreign");
        UUID published = report(owner, "PUBLISHED", START, "Русский комментарий\nSecond line", "План");
        UUID draft = report(owner, "DRAFT", END, "Draft must stay private", "Plan");
        UUID archived = report(owner, "ARCHIVED", END.plusSeconds(10), "Archived", "Plan");
        String url = REPORTS_URL + "/" + published + "/pdf";

        MvcResult first = mockMvc.perform(get(url).with(user(owner.teacherPrincipal())))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string("Content-Disposition",
                "attachment; filename=\"progress-report-" + published + ".pdf\""))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andReturn();
        String firstText = pdfText(first);
        assertThat(firstText).contains(
            "Отчёт о прогрессе", "Русский комментарий", "Second line", "План", "Нет оценки"
        );

        jdbc.update("insert into lesson_sessions(id, student_program_id, teacher_id, started_at, duration_minutes, attendance_status, private_notes) values (?, ?, ?, ?, 500, 'ATTENDED', 'DO_NOT_LEAK_PRIVATE_NOTES')",
            UUID.randomUUID(), owner.studentProgramId(), owner.teacherId(), Timestamp.from(END));
        MvcResult second = mockMvc.perform(get(url).with(user(owner.teacherPrincipal())))
            .andExpect(status().isOk()).andReturn();
        assertThat(pdfText(second)).isEqualTo(firstText).doesNotContain("DO_NOT_LEAK_PRIVATE_NOTES");

        mockMvc.perform(get(REPORTS_URL + "/" + draft + "/pdf")
                .with(user(owner.teacherPrincipal())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PROGRESS_REPORT_PDF_NOT_AVAILABLE"));
        mockMvc.perform(get(REPORTS_URL + "/" + archived + "/pdf")
                .with(user(owner.teacherPrincipal())))
            .andExpect(status().isConflict());
        mockMvc.perform(get(url).with(user(foreign.teacherPrincipal())))
            .andExpect(status().isNotFound());
        mockMvc.perform(get(url)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(url).with(user(new AuthenticatedUser(
            UUID.randomUUID(), "student@example.com", "", true,
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        )))).andExpect(status().isForbidden());
    }

    @Test
    void patchHasTruePatchSemanticsPreservesSnapshotAndRejectsStaleOrPublishedChanges() throws Exception {
        Fixture fixture = fixture("patch");
        UUID reportId = report(fixture, "DRAFT", START, "Old", "Keep");

        mockMvc.perform(patch(REPORTS_URL + "/" + reportId)
                .with(user(fixture.teacherPrincipal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"teacherSummary\":null,\"version\":0}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.teacherSummary").value((Object) null))
            .andExpect(jsonPath("$.nextPeriodPlan").value("Keep"))
            .andExpect(jsonPath("$.learningMinutes").value(10))
            .andExpect(jsonPath("$.snapshot.metrics.learningMinutes").value(10))
            .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(patch(REPORTS_URL + "/" + reportId)
                .with(user(fixture.teacherPrincipal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"teacherSummary\":\"Stale\",\"version\":0}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("OPTIMISTIC_LOCK_CONFLICT"));
        mockMvc.perform(get(REPORTS_URL + "/" + reportId)
                .with(user(fixture.teacherPrincipal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.teacherSummary").value((Object) null))
            .andExpect(jsonPath("$.nextPeriodPlan").value("Keep"))
            .andExpect(jsonPath("$.version").value(1));

        UUID published = report(fixture, "PUBLISHED", END, "Published", "Plan");
        mockMvc.perform(patch(REPORTS_URL + "/" + published)
                .with(user(fixture.teacherPrincipal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"teacherSummary\":\"No\",\"version\":0}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PROGRESS_REPORT_NOT_EDITABLE"));
    }

    @Test
    void publishIsAtomicPreservesTextAndSnapshotAndEnforcesOwnershipAndVersion() throws Exception {
        Fixture owner = fixture("publish-owner");
        Fixture foreign = fixture("publish-foreign");
        UUID reportId = report(owner, "DRAFT", START, "Summary", "Plan");

        mockMvc.perform(post(REPORTS_URL + "/" + reportId + "/publish")
                .with(user(foreign.teacherPrincipal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
            .andExpect(status().isNotFound());
        mockMvc.perform(post(REPORTS_URL + "/" + reportId + "/publish")
                .with(user(owner.teacherPrincipal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PUBLISHED"))
            .andExpect(jsonPath("$.publishedAt").isNotEmpty())
            .andExpect(jsonPath("$.teacherSummary").value("Summary"))
            .andExpect(jsonPath("$.nextPeriodPlan").value("Plan"))
            .andExpect(jsonPath("$.snapshot.metrics.learningMinutes").value(10));
        mockMvc.perform(post(REPORTS_URL + "/" + reportId + "/publish")
                .with(user(owner.teacherPrincipal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":1}"))
            .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject(
            "select count(*) from report_shares where report_id = ?", Integer.class, reportId
        )).isZero();
    }

    @Test
    void securityCsrfAndOpenApiContractAreExposed() throws Exception {
        Fixture fixture = fixture("security");
        UUID reportId = report(fixture, "DRAFT", START, null, null);
        AuthenticatedUser student = new AuthenticatedUser(
            UUID.randomUUID(), "student@example.com", "", true,
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        mockMvc.perform(get(REPORTS_URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(REPORTS_URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(REPORTS_URL).with(user(student))).andExpect(status().isForbidden());
        mockMvc.perform(get(REPORTS_URL).with(user(fixture.teacherPrincipal())))
            .andExpect(status().isOk());
        mockMvc.perform(post(REPORTS_URL)
                .with(user(fixture.teacherPrincipal()))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
        mockMvc.perform(patch(REPORTS_URL + "/" + reportId)
                .with(user(fixture.teacherPrincipal()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
            .andExpect(status().isForbidden());
        mockMvc.perform(post(REPORTS_URL + "/" + reportId + "/publish")
                .with(user(fixture.teacherPrincipal()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/v1/teacher/reports'].post.operationId").value("createProgressReport"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/reports'].get.operationId").value("listProgressReports"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/reports/{reportId}'].get.operationId").value("getProgressReport"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/reports/{reportId}'].patch.operationId").value("updateProgressReport"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/reports/{reportId}/pdf'].get.operationId").value("downloadProgressReportPdf"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/reports/{reportId}/pdf'].get.responses['200'].content['application/pdf'].schema.format").value("binary"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/reports/{reportId}/publish'].post.operationId").value("publishProgressReport"))
            .andExpect(jsonPath("$.components.schemas.ProgressReportDetailsResponse.properties.snapshotSchemaVersion").exists())
            .andExpect(jsonPath("$.components.schemas.ProgressReportSnapshotV1").exists())
            .andExpect(jsonPath("$.components.schemas.ProgressReportSummaryResponse.properties.snapshot").doesNotExist())
            .andExpect(jsonPath("$.components.schemas.ApiError").exists());
    }

    private org.springframework.test.web.servlet.ResultActions performCreate(
        Fixture fixture, UUID studentProgramId, UUID learningPeriodId
    ) throws Exception {
        return mockMvc.perform(post(REPORTS_URL)
            .with(user(fixture.teacherPrincipal())).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"studentProgramId\":\"%s\",\"learningPeriodId\":\"%s\"}"
                .formatted(studentProgramId, learningPeriodId)));
    }

    private Fixture fixture(String suffix) {
        UUID userId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID learningProgramId = UUID.randomUUID();
        UUID studentProgramId = UUID.randomUUID();
        jdbc.update("insert into users(id, email) values (?, ?)", userId, suffix + userId + "@example.com");
        jdbc.update("insert into teachers(id, user_id, display_name) values (?, ?, 'Teacher')", teacherId, userId);
        jdbc.update("insert into students(id, first_name) values (?, 'Student')", studentId);
        jdbc.update("insert into teacher_student_links(teacher_id, student_id) values (?, ?)", teacherId, studentId);
        jdbc.update("insert into subjects(id, owner_teacher_id, name) values (?, ?, 'Subject')", subjectId, teacherId);
        jdbc.update("insert into learning_programs(id, teacher_id, subject_id, title, status) values (?, ?, ?, 'Program', 'ACTIVE')", learningProgramId, teacherId, subjectId);
        jdbc.update("insert into student_programs(id, student_id, learning_program_id, assigned_by_teacher_id) values (?, ?, ?, ?)", studentProgramId, studentId, learningProgramId, teacherId);
        return new Fixture(userId, teacherId, studentId, studentProgramId);
    }

    private UUID period(Fixture fixture, String status, UUID studentProgramId) {
        UUID id = UUID.randomUUID();
        boolean completed = status.equals("COMPLETED");
        jdbc.update("""
            insert into learning_periods(
              id, student_program_id, sequence_no, start_cumulative_minutes,
              target_duration_minutes, end_cumulative_minutes, status, started_at, completed_at
            ) values (?, ?, 1, 0, 10, ?, ?, ?, ?)
            """, id, studentProgramId, completed ? 10 : null, status,
            completed ? Timestamp.from(START) : null, completed ? Timestamp.from(END) : null);
        return id;
    }

    private UUID report(Fixture fixture, String status, Instant createdAt, String summary, String plan) {
        UUID id = UUID.randomUUID();
        Instant publishedAt = status.equals("PUBLISHED") ? createdAt.plusSeconds(1) : null;
        jdbc.update("""
            insert into progress_reports(
              id, student_program_id, generated_by_teacher_id, status,
              period_started_at, period_ended_at, learning_minutes,
              snapshot_schema_version, snapshot_json, teacher_summary, next_period_plan,
              published_at, created_at, updated_at
            ) values (?, ?, ?, ?, ?, ?, 10, 1, cast(? as jsonb), ?, ?, ?, ?, ?)
            """, id, fixture.studentProgramId(), fixture.teacherId(), status,
            Timestamp.from(START), Timestamp.from(END), SNAPSHOT, summary, plan,
            publishedAt == null ? null : Timestamp.from(publishedAt),
            Timestamp.from(createdAt), Timestamp.from(createdAt));
        return id;
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private String pdfText(MvcResult result) throws Exception {
        try (var document = Loader.loadPDF(result.getResponse().getContentAsByteArray())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private record Fixture(UUID userId, UUID teacherId, UUID studentId, UUID studentProgramId) {
        AuthenticatedUser teacherPrincipal() {
            return new AuthenticatedUser(
                userId, "teacher@example.com", "", true,
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
            );
        }
    }
}
