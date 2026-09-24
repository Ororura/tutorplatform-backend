package com.tutorplatform.dashboard.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TeacherDashboardApiIntegrationTest extends PostgresIntegrationTest {

    private static final String DASHBOARD_URL = "/api/v1/teacher/dashboard";

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_teacher_dashboard_api", "008");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @MockitoSpyBean private JdbcClient jdbcClient;

    @Test
    void emptyDashboardReturnsZeroCountsAndNoAttentionItems() throws Exception {
        TeacherFixture teacher = createTeacher("empty");

        mockMvc.perform(get(DASHBOARD_URL).with(user(teacher.principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeStudentsCount").value(0))
                .andExpect(jsonPath("$.needsReviewSubmissionsCount").value(0))
                .andExpect(jsonPath("$.overdueHomeworksCount").value(0))
                .andExpect(
                        jsonPath("$.completedLearningPeriodsWithoutPublishedReportCount").value(0))
                .andExpect(jsonPath("$.attentionItems").isEmpty());
    }

    @Test
    void oneStudentIsIncludedInActiveStudentCount() throws Exception {
        TeacherFixture teacher = createTeacher("one-student");
        createStudent(teacher, "Анна", null);

        mockMvc.perform(get(DASHBOARD_URL).with(user(teacher.principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeStudentsCount").value(1))
                .andExpect(jsonPath("$.attentionItems").isEmpty());
    }

    @Test
    void multipleStudentsAreAggregatedWithoutDuplication() throws Exception {
        TeacherFixture teacher = createTeacher("multiple-students");
        StudentFixture first = createStudent(teacher, "Анна", "Иванова");
        StudentFixture second = createStudent(teacher, "Борис", "Петров");
        createNeedsReviewSubmission(first, 1, Instant.parse("2026-09-20T10:00:00Z"));
        createNeedsReviewSubmission(second, 1, Instant.parse("2026-09-20T11:00:00Z"));

        mockMvc.perform(get(DASHBOARD_URL).with(user(teacher.principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeStudentsCount").value(2))
                .andExpect(jsonPath("$.needsReviewSubmissionsCount").value(2))
                .andExpect(jsonPath("$.attentionItems.length()").value(2));
    }

    @Test
    void overdueUsesDueAtAndAssignedStatus() throws Exception {
        TeacherFixture teacher = createTeacher("overdue");
        StudentFixture student = createStudent(teacher, "Ирина", "Соколова");
        UUID overdue =
                createHomework(
                        student,
                        "Просроченное ДЗ",
                        Instant.parse("2020-01-01T10:00:00Z"),
                        "ASSIGNED");
        createHomework(student, "Будущее ДЗ", Instant.parse("2099-01-01T10:00:00Z"), "ASSIGNED");
        createHomework(
                student, "Завершённое ДЗ", Instant.parse("2020-01-02T10:00:00Z"), "COMPLETED");

        mockMvc.perform(get(DASHBOARD_URL).with(user(teacher.principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdueHomeworksCount").value(1))
                .andExpect(jsonPath("$.attentionItems.length()").value(1))
                .andExpect(jsonPath("$.attentionItems[0].type").value("HOMEWORK_OVERDUE"))
                .andExpect(jsonPath("$.attentionItems[0].studentId").value(student.id().toString()))
                .andExpect(jsonPath("$.attentionItems[0].displayName").value("Ирина Соколова"))
                .andExpect(jsonPath("$.attentionItems[0].resourceId").value(overdue.toString()))
                .andExpect(
                        jsonPath("$.attentionItems[0].navigation.studentProgramId")
                                .value(student.studentProgramId().toString()))
                .andExpect(
                        jsonPath("$.attentionItems[0].navigation.homeworkId")
                                .value(overdue.toString()));
    }

    @Test
    void needsReviewSubmissionIsCountedAndContainsNavigationData() throws Exception {
        TeacherFixture teacher = createTeacher("needs-review");
        StudentFixture student = createStudent(teacher, "Мария", "Орлова");
        UUID submission =
                createNeedsReviewSubmission(student, 1, Instant.parse("2026-09-21T12:30:00Z"));
        createSubmission(student, 2, "PASSED", Instant.parse("2026-09-21T12:31:00Z"));

        mockMvc.perform(get(DASHBOARD_URL).with(user(teacher.principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needsReviewSubmissionsCount").value(1))
                .andExpect(jsonPath("$.attentionItems.length()").value(1))
                .andExpect(jsonPath("$.attentionItems[0].type").value("SUBMISSION_NEEDS_REVIEW"))
                .andExpect(jsonPath("$.attentionItems[0].resourceId").value(submission.toString()))
                .andExpect(jsonPath("$.attentionItems[0].eventAt").value("2026-09-21T12:30:00Z"))
                .andExpect(
                        jsonPath("$.attentionItems[0].navigation.submissionId")
                                .value(submission.toString()))
                .andExpect(
                        jsonPath("$.attentionItems[0].navigation.taskId")
                                .value(student.taskId().toString()));
    }

    @Test
    void completedPeriodsWithoutPublishedReportAreCounted() throws Exception {
        TeacherFixture teacher = createTeacher("missing-report");
        StudentFixture student = createStudent(teacher, "Олег", "Миронов");
        UUID withoutReport = createCompletedLearningPeriod(student, 1, 480);
        UUID withDraft = createCompletedLearningPeriod(student, 2, 960);
        UUID draftReport = createReport(student, withDraft, "DRAFT");
        UUID withPublished = createCompletedLearningPeriod(student, 3, 1440);
        createReport(student, withPublished, "PUBLISHED");

        mockMvc.perform(get(DASHBOARD_URL).with(user(teacher.principal())))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.completedLearningPeriodsWithoutPublishedReportCount").value(2))
                .andExpect(jsonPath("$.attentionItems.length()").value(2))
                .andExpect(
                        jsonPath("$.attentionItems[?(@.resourceId == '%s')]", withoutReport)
                                .exists())
                .andExpect(
                        jsonPath(
                                        "$.attentionItems[?(@.resourceId == '%s')].navigation.reportId",
                                        withDraft)
                                .value(draftReport.toString()));
    }

    @Test
    void anotherTeachersStudentsAndEventsAreExcluded() throws Exception {
        TeacherFixture currentTeacher = createTeacher("scope-owner");
        createStudent(currentTeacher, "Свой", "Ученик");
        TeacherFixture anotherTeacher = createTeacher("scope-foreign");
        StudentFixture foreignStudent = createStudent(anotherTeacher, "Чужой", "Ученик");
        createHomework(
                foreignStudent,
                "Чужое просроченное ДЗ",
                Instant.parse("2020-01-01T10:00:00Z"),
                "ASSIGNED");
        createNeedsReviewSubmission(foreignStudent, 1, Instant.parse("2026-09-21T12:30:00Z"));

        mockMvc.perform(get(DASHBOARD_URL).with(user(currentTeacher.principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeStudentsCount").value(1))
                .andExpect(jsonPath("$.needsReviewSubmissionsCount").value(0))
                .andExpect(jsonPath("$.overdueHomeworksCount").value(0))
                .andExpect(jsonPath("$.attentionItems").isEmpty());
    }

    @Test
    void attentionItemsAreLimitedAndDashboardUsesTwoReadQueries() throws Exception {
        TeacherFixture teacher = createTeacher("bounded");
        StudentFixture student = createStudent(teacher, "Много", "Событий");
        Instant base = Instant.parse("2026-09-01T00:00:00Z");
        for (int attempt = 1; attempt <= 25; attempt++) {
            createNeedsReviewSubmission(student, attempt, base.plusSeconds(attempt));
        }
        clearInvocations(jdbcClient);

        mockMvc.perform(get(DASHBOARD_URL).with(user(teacher.principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needsReviewSubmissionsCount").value(25))
                .andExpect(jsonPath("$.attentionItems.length()").value(20));

        verify(jdbcClient, times(2)).sql(anyString());
    }

    @Test
    void openApiPublishesStableOperationId() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.paths['/api/v1/teacher/dashboard'].get.operationId")
                                .value("getTeacherDashboard"));
    }

    private TeacherFixture createTeacher(String marker) {
        UUID userId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        String email = marker + "-" + userId + "@example.com";
        jdbc.update(
                "INSERT INTO users(id, email, password_hash, status) VALUES (?, ?, ?, 'ACTIVE')",
                userId,
                email,
                "password-hash");
        jdbc.update("INSERT INTO user_roles(user_id, role) VALUES (?, 'TEACHER')", userId);
        jdbc.update(
                "INSERT INTO teachers(id, user_id, display_name) VALUES (?, ?, ?)",
                teacherId,
                userId,
                "Teacher " + marker);
        var principal =
                new AuthenticatedUser(
                        userId,
                        email,
                        "password-hash",
                        true,
                        List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
        return new TeacherFixture(teacherId, principal);
    }

    private StudentFixture createStudent(
            TeacherFixture teacher, String firstName, String lastName) {
        UUID studentId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID learningProgramId = UUID.randomUUID();
        UUID studentProgramId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO students(id, first_name, last_name, status) VALUES (?, ?, ?, 'ACTIVE')",
                studentId,
                firstName,
                lastName);
        jdbc.update(
                "INSERT INTO teacher_student_links(teacher_id, student_id) VALUES (?, ?)",
                teacher.id(),
                studentId);
        jdbc.update(
                "INSERT INTO subjects(id, owner_teacher_id, name, status) VALUES (?, ?, ?, 'ACTIVE')",
                subjectId,
                teacher.id(),
                "Subject " + subjectId);
        jdbc.update(
                "INSERT INTO learning_programs(id, teacher_id, subject_id, title, status) VALUES (?, ?, ?, ?, 'ACTIVE')",
                learningProgramId,
                teacher.id(),
                subjectId,
                "Program " + studentId);
        jdbc.update(
                "INSERT INTO student_programs(id, student_id, learning_program_id, assigned_by_teacher_id, status) VALUES (?, ?, ?, ?, 'ACTIVE')",
                studentProgramId,
                studentId,
                learningProgramId,
                teacher.id());
        jdbc.update(
                "INSERT INTO tasks(id, teacher_id, subject_id, title, description_markdown, task_type, status) VALUES (?, ?, ?, ?, ?, 'TEXT', 'ACTIVE')",
                taskId,
                teacher.id(),
                subjectId,
                "Task " + studentId,
                "Description");
        return new StudentFixture(
                teacher.id(), studentId, studentProgramId, taskId, firstName, lastName);
    }

    private UUID createHomework(
            StudentFixture student, String title, Instant dueAt, String status) {
        UUID homeworkId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO homeworks(id, student_program_id, assigned_by_teacher_id, title, assigned_at, due_at, status, completed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                homeworkId,
                student.studentProgramId(),
                student.teacherId(),
                title,
                Timestamp.from(dueAt.minusSeconds(3600)),
                Timestamp.from(dueAt),
                status,
                status.equals("COMPLETED") ? Timestamp.from(dueAt.plusSeconds(60)) : null);
        return homeworkId;
    }

    private UUID createNeedsReviewSubmission(
            StudentFixture student, int attempt, Instant submittedAt) {
        return createSubmission(student, attempt, "NEEDS_REVIEW", submittedAt);
    }

    private UUID createSubmission(
            StudentFixture student, int attempt, String status, Instant submittedAt) {
        UUID submissionId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO submissions(id, student_id, student_program_id, task_id, attempt_no, status, text_answer, submitted_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                submissionId,
                student.id(),
                student.studentProgramId(),
                student.taskId(),
                attempt,
                status,
                "Answer " + attempt,
                Timestamp.from(submittedAt));
        return submissionId;
    }

    private UUID createCompletedLearningPeriod(
            StudentFixture student, int sequence, int endMinutes) {
        UUID periodId = UUID.randomUUID();
        Instant completedAt = Instant.parse("2026-09-01T00:00:00Z").plusSeconds(sequence);
        jdbc.update(
                "INSERT INTO learning_periods(id, student_program_id, sequence_no, start_cumulative_minutes, target_duration_minutes, end_cumulative_minutes, status, started_at, completed_at) VALUES (?, ?, ?, ?, 480, ?, 'COMPLETED', ?, ?)",
                periodId,
                student.studentProgramId(),
                sequence,
                endMinutes - 480,
                endMinutes,
                Timestamp.from(completedAt.minusSeconds(3600)),
                Timestamp.from(completedAt));
        return periodId;
    }

    private UUID createReport(StudentFixture student, UUID learningPeriodId, String status) {
        UUID reportId = UUID.randomUUID();
        Instant periodStartedAt = Instant.parse("2026-08-01T00:00:00Z");
        Instant periodEndedAt = Instant.parse("2026-09-01T00:00:00Z");
        jdbc.update(
                "INSERT INTO progress_reports(id, student_program_id, learning_period_id, generated_by_teacher_id, status, period_started_at, period_ended_at, learning_minutes, published_at) VALUES (?, ?, ?, ?, ?, ?, ?, 480, ?)",
                reportId,
                student.studentProgramId(),
                learningPeriodId,
                student.teacherId(),
                status,
                Timestamp.from(periodStartedAt),
                Timestamp.from(periodEndedAt),
                status.equals("PUBLISHED") ? Timestamp.from(periodEndedAt.plusSeconds(60)) : null);
        return reportId;
    }

    private record TeacherFixture(UUID id, AuthenticatedUser principal) {}

    private record StudentFixture(
            UUID teacherId,
            UUID id,
            UUID studentProgramId,
            UUID taskId,
            String firstName,
            String lastName) {}
}
