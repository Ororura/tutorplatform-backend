package com.tutorplatform.report.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.test.PostgresIntegrationTest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LearningPeriodApiIntegrationTest extends PostgresIntegrationTest {

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_learning_period_read_api", "008");
    }

    private static final Instant STARTED_AT = Instant.parse("2026-02-01T10:00:00Z");
    private static final Instant COMPLETED_AT = Instant.parse("2026-02-28T11:30:00Z");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void requiresAuthenticationAndTeacherRole() throws Exception {
        Fixture fixture = fixture("security");

        mockMvc.perform(get(url(fixture))).andExpect(status().isUnauthorized());
        mockMvc.perform(
                        get(url(fixture))
                                .with(
                                        user(
                                                new AuthenticatedUser(
                                                        fixture.userId(),
                                                        "student@example.com",
                                                        "",
                                                        true,
                                                        List.of(
                                                                new SimpleGrantedAuthority(
                                                                        "ROLE_STUDENT"))))))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsEmptyListWithoutCreatingPeriods() throws Exception {
        Fixture fixture = fixture("empty");

        mockMvc.perform(get(url(fixture)).with(user(fixture.teacherPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from learning_periods where student_program_id = ?",
                                Integer.class,
                                fixture.studentProgramId()))
                .isZero();
    }

    @Test
    void returnsActivePeriodFromPersistedFacts() throws Exception {
        Fixture fixture = fixture("active");
        UUID periodId =
                period(fixture.studentProgramId(), 1, 0, 480, null, "ACTIVE", STARTED_AT, null);

        mockMvc.perform(get(url(fixture)).with(user(fixture.teacherPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(periodId.toString()))
                .andExpect(jsonPath("$[0].sequenceNo").value(1))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].startCumulativeMinutes").value(0))
                .andExpect(jsonPath("$[0].endCumulativeMinutes").value((Object) null))
                .andExpect(jsonPath("$[0].targetDurationMinutes").value(480))
                .andExpect(jsonPath("$[0].startedAt").value(STARTED_AT.toString()))
                .andExpect(jsonPath("$[0].completedAt").value((Object) null))
                .andExpect(jsonPath("$[0].reportId").value((Object) null));
    }

    @Test
    void returnsCompletedPeriodFromPersistedFacts() throws Exception {
        Fixture fixture = fixture("completed");
        UUID periodId =
                period(
                        fixture.studentProgramId(),
                        1,
                        0,
                        480,
                        500,
                        "COMPLETED",
                        STARTED_AT,
                        COMPLETED_AT);

        mockMvc.perform(get(url(fixture)).with(user(fixture.teacherPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(periodId.toString()))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].endCumulativeMinutes").value(500))
                .andExpect(jsonPath("$[0].completedAt").value(COMPLETED_AT.toString()));
    }

    @Test
    void includesExistingReportId() throws Exception {
        Fixture fixture = fixture("report");
        UUID periodId =
                period(
                        fixture.studentProgramId(),
                        1,
                        0,
                        480,
                        500,
                        "COMPLETED",
                        STARTED_AT,
                        COMPLETED_AT);
        UUID reportId = report(fixture, periodId);

        mockMvc.perform(get(url(fixture)).with(user(fixture.teacherPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(periodId.toString()))
                .andExpect(jsonPath("$[0].reportId").value(reportId.toString()));
    }

    @Test
    void returnsPeriodsInSequenceOrder() throws Exception {
        Fixture fixture = fixture("sequence");
        UUID second = period(fixture.studentProgramId(), 2, 480, 480, null, "ACTIVE", null, null);
        UUID first =
                period(
                        fixture.studentProgramId(),
                        1,
                        0,
                        480,
                        480,
                        "COMPLETED",
                        STARTED_AT,
                        COMPLETED_AT);

        mockMvc.perform(get(url(fixture)).with(user(fixture.teacherPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(first.toString()))
                .andExpect(jsonPath("$[1].id").value(second.toString()));
    }

    @Test
    void hidesProgramWhenStudentPathBelongsToAnotherStudent() throws Exception {
        Fixture owner = fixture("student-owner");
        Fixture other = fixture("student-other");
        period(owner.studentProgramId(), 1, 0, 480, null, "ACTIVE", null, null);

        mockMvc.perform(
                        get(url(other.studentId(), owner.studentProgramId()))
                                .with(user(owner.teacherPrincipal())))
                .andExpect(status().isNotFound());
    }

    @Test
    void hidesProgramOwnedByAnotherTeacher() throws Exception {
        Fixture owner = fixture("program-owner");
        Fixture other = fixture("program-other");
        period(other.studentProgramId(), 1, 0, 480, null, "ACTIVE", null, null);

        mockMvc.perform(
                        get(url(owner.studentId(), other.studentProgramId()))
                                .with(user(owner.teacherPrincipal())))
                .andExpect(status().isNotFound());
    }

    @Test
    void openApiPublishesLearningPeriodOperation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/v1/teacher/students/{studentId}/programs/{studentProgramId}/learning-periods'].get.operationId")
                                .value("listTeacherStudentLearningPeriods"))
                .andExpect(
                        jsonPath(
                                        "$.components.schemas.LearningPeriodResponse.properties.reportId.format")
                                .value("uuid"));
    }

    private Fixture fixture(String suffix) {
        UUID userId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID learningProgramId = UUID.randomUUID();
        UUID studentProgramId = UUID.randomUUID();
        jdbc.update(
                "insert into users(id, email) values (?, ?)",
                userId,
                suffix + userId + "@example.com");
        jdbc.update(
                "insert into teachers(id, user_id, display_name) values (?, ?, 'Teacher')",
                teacherId,
                userId);
        jdbc.update("insert into students(id, first_name) values (?, 'Student')", studentId);
        jdbc.update(
                "insert into teacher_student_links(teacher_id, student_id) values (?, ?)",
                teacherId,
                studentId);
        jdbc.update(
                "insert into subjects(id, owner_teacher_id, name) values (?, ?, 'Subject')",
                subjectId,
                teacherId);
        jdbc.update(
                "insert into learning_programs(id, teacher_id, subject_id, title, status) values (?, ?, ?, 'Program', 'ACTIVE')",
                learningProgramId,
                teacherId,
                subjectId);
        jdbc.update(
                "insert into student_programs(id, student_id, learning_program_id, assigned_by_teacher_id) values (?, ?, ?, ?)",
                studentProgramId,
                studentId,
                learningProgramId,
                teacherId);
        return new Fixture(userId, teacherId, studentId, studentProgramId);
    }

    private UUID period(
            UUID studentProgramId,
            int sequenceNo,
            int startMinutes,
            int targetMinutes,
            Integer endMinutes,
            String status,
            Instant startedAt,
            Instant completedAt) {
        UUID periodId = UUID.randomUUID();
        jdbc.update(
                """
            insert into learning_periods(
              id, student_program_id, sequence_no, start_cumulative_minutes,
              target_duration_minutes, end_cumulative_minutes, status, started_at, completed_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
                periodId,
                studentProgramId,
                sequenceNo,
                startMinutes,
                targetMinutes,
                endMinutes,
                status,
                timestamp(startedAt),
                timestamp(completedAt));
        return periodId;
    }

    private UUID report(Fixture fixture, UUID periodId) {
        UUID reportId = UUID.randomUUID();
        jdbc.update(
                """
            insert into progress_reports(
              id, student_program_id, learning_period_id, generated_by_teacher_id,
              period_started_at, period_ended_at, learning_minutes,
              snapshot_schema_version, snapshot_json
            ) values (?, ?, ?, ?, ?, ?, 500, 1, '{}'::jsonb)
            """,
                reportId,
                fixture.studentProgramId(),
                periodId,
                fixture.teacherId(),
                Timestamp.from(STARTED_AT),
                Timestamp.from(COMPLETED_AT));
        return reportId;
    }

    private Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private String url(Fixture fixture) {
        return url(fixture.studentId(), fixture.studentProgramId());
    }

    private String url(UUID studentId, UUID studentProgramId) {
        return "/api/v1/teacher/students/"
                + studentId
                + "/programs/"
                + studentProgramId
                + "/learning-periods";
    }

    private record Fixture(UUID userId, UUID teacherId, UUID studentId, UUID studentProgramId) {
        AuthenticatedUser teacherPrincipal() {
            return new AuthenticatedUser(
                    userId,
                    "teacher@example.com",
                    "",
                    true,
                    List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
        }
    }
}
