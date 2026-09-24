package com.tutorplatform.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.progress.application.GetCurrentProgressService;
import com.tutorplatform.progress.application.ProgressInterval;
import com.tutorplatform.report.application.exception.*;
import com.tutorplatform.report.domain.ProgressReport;
import com.tutorplatform.report.domain.ProgressReportSnapshotV1;
import com.tutorplatform.report.domain.ProgressReportStatus;
import com.tutorplatform.test.PostgresIntegrationTest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProgressReportApplicationIntegrationTest extends PostgresIntegrationTest {

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(
                registry, "test_progress_report_application", "008");
    }

    private static final Instant BASE = Instant.parse("2026-01-01T10:00:00Z");

    @Autowired private CreateProgressReportDraft createDraft;
    @Autowired private EditProgressReportDraft editDraft;
    @Autowired private PublishProgressReport publishReport;
    @Autowired private ProgressReportQueryService queryService;
    @Autowired private GetCurrentProgressService currentProgressService;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void completedPeriodCreatesImmutableIntervalSnapshotAndSupportsEditPublishAndRead() {
        Fixture fixture = fixture();
        Instant periodEnd = BASE.plusSeconds(7200);
        UUID periodId = period(fixture, 1, 0, 480, 510, "COMPLETED", BASE, periodEnd);
        Facts facts = facts(fixture, BASE, periodEnd);
        int sourceFactCount = sourceFactCount(fixture.studentProgramId());

        ProgressReport draft =
                createDraft.create(
                        fixture.principal(),
                        fixture.studentId(),
                        fixture.studentProgramId(),
                        periodId);

        assertThat(draft.status()).isEqualTo(ProgressReportStatus.DRAFT);
        assertThat(draft.generatedByTeacherId()).isEqualTo(fixture.teacherId());
        assertThat(draft.publishedAt()).isNull();
        assertThat(draft.periodStartedAt()).isEqualTo(BASE);
        assertThat(draft.periodEndedAt()).isEqualTo(periodEnd);
        assertThat(draft.learningMinutes()).isEqualTo(510);
        assertThat(draft.snapshot().metrics())
                .isEqualTo(new ProgressReportSnapshotV1.Metrics(510, 3, 2.0 / 3.0, 2, 1, 2, 1));
        assertThat(draft.snapshot().assessment().understandingAverage())
                .isEqualByComparingTo("4.0");
        assertThat(draft.snapshot().assessment().independenceAverage()).isEqualByComparingTo("4.0");
        assertThat(draft.snapshot().assessment().practiceAverage()).isEqualByComparingTo("5.0");
        assertThat(draft.snapshot().assessment().homeworkAverage()).isEqualByComparingTo("4.0");
        assertThat(draft.snapshot().topics().completed())
                .extracting(ProgressReportSnapshotV1.Topic::title)
                .containsExactly("Условия");
        assertThat(draft.snapshot().topics().inProgress())
                .extracting(ProgressReportSnapshotV1.Topic::title)
                .containsExactly("Циклы");
        assertThat(draft.snapshot().skills()).isEmpty();

        ProgressReport edited =
                editDraft.edit(
                        fixture.principal(),
                        fixture.studentId(),
                        fixture.studentProgramId(),
                        draft.id(),
                        new EditProgressReportDraftCommand("Summary", "Plan", draft.version()));
        assertThat(edited.teacherSummary()).isEqualTo("Summary");
        assertThat(edited.nextPeriodPlan()).isEqualTo("Plan");
        assertThat(edited.snapshot()).isEqualTo(draft.snapshot());
        assertThat(edited.version()).isEqualTo(draft.version() + 1);

        assertThatThrownBy(
                        () ->
                                editDraft.edit(
                                        fixture.principal(),
                                        fixture.studentId(),
                                        fixture.studentProgramId(),
                                        draft.id(),
                                        new EditProgressReportDraftCommand(
                                                "Stale", null, draft.version())))
                .isInstanceOf(ProgressReportVersionConflictException.class);

        ProgressReport published =
                publishReport.publish(
                        fixture.principal(),
                        fixture.studentId(),
                        fixture.studentProgramId(),
                        edited.id(),
                        edited.version());
        assertThat(published.status()).isEqualTo(ProgressReportStatus.PUBLISHED);
        assertThat(published.publishedAt()).isNotNull();
        assertThat(published.snapshot()).isEqualTo(draft.snapshot());
        assertThat(published.learningMinutes()).isEqualTo(510);
        assertThat(published.periodStartedAt()).isEqualTo(BASE);
        assertThat(published.periodEndedAt()).isEqualTo(periodEnd);

        assertThatThrownBy(
                        () ->
                                publishReport.publish(
                                        fixture.principal(),
                                        fixture.studentId(),
                                        fixture.studentProgramId(),
                                        published.id(),
                                        published.version()))
                .isInstanceOf(InvalidProgressReportStateException.class);
        assertThatThrownBy(
                        () ->
                                editDraft.edit(
                                        fixture.principal(),
                                        fixture.studentId(),
                                        fixture.studentProgramId(),
                                        published.id(),
                                        new EditProgressReportDraftCommand(
                                                "Changed", "Changed", published.version())))
                .isInstanceOf(InvalidProgressReportStateException.class);

        ProgressReportSnapshotV1 historical = published.snapshot();
        jdbc.update("UPDATE topics SET title = 'Renamed' WHERE id = ?", facts.completedTopicId());
        session(fixture, periodEnd.plusSeconds(3600), 60, "ATTENDED");
        submission(
                fixture, facts.taskId(), facts.itemId(), 3, "PASSED", periodEnd.plusSeconds(3600));
        jdbc.update(
                "UPDATE teacher_assessments SET understanding_score = 1 WHERE id = ?",
                facts.assessmentId());
        jdbc.update(
                "UPDATE homeworks SET status = 'COMPLETED', completed_at = ? WHERE student_program_id = ? AND status = 'ASSIGNED'",
                Timestamp.from(periodEnd.minusSeconds(30)),
                fixture.studentProgramId());

        assertThat(
                        currentProgressService
                                .getProgressSnapshot(
                                        fixture.studentProgramId(),
                                        new ProgressInterval(BASE, periodEnd))
                                .homeworkCompleted())
                .isEqualTo(2);

        ProgressReport readAgain =
                queryService.get(
                        fixture.principal(),
                        fixture.studentId(),
                        fixture.studentProgramId(),
                        published.id());
        assertThat(readAgain.snapshot()).isEqualTo(historical);
        assertThat(sourceFactCount(fixture.studentProgramId())).isGreaterThan(sourceFactCount);
        assertThat(
                        queryService
                                .list(
                                        fixture.principal(),
                                        fixture.studentId(),
                                        fixture.studentProgramId(),
                                        0,
                                        10)
                                .items())
                .extracting(ProgressReport::id)
                .containsExactly(published.id());
    }

    @Test
    void rejectsActiveMismatchAndDuplicatePeriods() {
        Fixture fixture = fixture();
        UUID active = period(fixture, 1, 0, 480, null, "ACTIVE", null, null);
        assertThatThrownBy(
                        () ->
                                createDraft.create(
                                        fixture.principal(),
                                        fixture.studentId(),
                                        fixture.studentProgramId(),
                                        active))
                .isInstanceOf(InvalidProgressReportPeriodException.class);

        Fixture other = fixture();
        UUID otherPeriod = period(other, 1, 0, 480, 480, "COMPLETED", BASE, BASE.plusSeconds(3600));
        assertThatThrownBy(
                        () ->
                                createDraft.create(
                                        fixture.principal(),
                                        fixture.studentId(),
                                        fixture.studentProgramId(),
                                        otherPeriod))
                .isInstanceOf(InvalidProgressReportPeriodException.class);

        jdbc.update("DELETE FROM learning_periods WHERE id = ?", active);
        UUID completed = period(fixture, 1, 0, 480, 510, "COMPLETED", BASE, BASE.plusSeconds(3600));
        session(fixture, BASE, 510, "ATTENDED");
        createDraft.create(
                fixture.principal(), fixture.studentId(), fixture.studentProgramId(), completed);
        assertThatThrownBy(
                        () ->
                                createDraft.create(
                                        fixture.principal(),
                                        fixture.studentId(),
                                        fixture.studentProgramId(),
                                        completed))
                .isInstanceOf(ProgressReportConflictException.class);
    }

    @Test
    void consecutivePeriodsDoNotLeakSessionOrLaterFactsIntoEarlierSnapshot() {
        Fixture fixture = fixture();
        Instant firstEnd = BASE.plusSeconds(3600);
        Instant secondStart = BASE.plusSeconds(7200);
        Instant secondEnd = BASE.plusSeconds(10800);
        UUID firstPeriod = period(fixture, 1, 0, 60, 60, "COMPLETED", BASE, firstEnd);
        UUID secondPeriod = period(fixture, 2, 60, 60, 120, "COMPLETED", secondStart, secondEnd);
        session(fixture, firstEnd, 60, "ATTENDED");
        session(fixture, secondStart, 60, "ATTENDED");

        ProgressReport first =
                createDraft.create(
                        fixture.principal(),
                        fixture.studentId(),
                        fixture.studentProgramId(),
                        firstPeriod);
        ProgressReport second =
                createDraft.create(
                        fixture.principal(),
                        fixture.studentId(),
                        fixture.studentProgramId(),
                        secondPeriod);

        assertThat(first.snapshot().metrics().sessionsCount()).isOne();
        assertThat(first.snapshot().metrics().learningMinutes()).isEqualTo(60);
        assertThat(second.snapshot().metrics().sessionsCount()).isOne();
        assertThat(second.snapshot().metrics().learningMinutes()).isEqualTo(60);
        assertThat(
                        first.snapshot().metrics().learningMinutes()
                                + second.snapshot().metrics().learningMinutes())
                .isEqualTo(
                        currentProgressService
                                .getCurrentProgress(fixture.studentProgramId())
                                .totalLearningMinutes());
        assertThat(second.snapshot().assessment())
                .isEqualTo(new ProgressReportSnapshotV1.Assessment(null, null, null, null));
        assertThat(
                        queryService
                                .get(
                                        fixture.principal(),
                                        fixture.studentId(),
                                        fixture.studentProgramId(),
                                        first.id())
                                .snapshot())
                .isEqualTo(first.snapshot());
    }

    private Facts facts(Fixture fixture, Instant start, Instant end) {
        UUID moduleId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO modules(id, learning_program_id, title, position) VALUES (?, ?, 'Module', 0)",
                moduleId,
                fixture.learningProgramId());
        UUID completedTopic =
                topic(fixture, moduleId, 0, "Условия", "COMPLETED", start, end.minusSeconds(300));
        topic(fixture, moduleId, 1, "Циклы", "IN_PROGRESS", start.plusSeconds(60), null);

        UUID firstSession = session(fixture, start, 60, "ATTENDED");
        UUID secondSession = session(fixture, start.plusSeconds(3600), 450, "ATTENDED");
        session(fixture, start.plusSeconds(1800), 45, "MISSED");
        UUID assessmentId = assessment(firstSession, 5, 4, null, 3);
        assessment(secondSession, 3, null, 5, 5);

        UUID completedHomework =
                homework(fixture, start.plusSeconds(600), "COMPLETED", end.minusSeconds(60));
        UUID assignedHomework = homework(fixture, start.plusSeconds(900), "ASSIGNED", null);
        UUID passedTask = task(fixture, "Passed");
        UUID pendingTask = task(fixture, "Pending");
        UUID passedItem = item(completedHomework, passedTask, 0);
        item(assignedHomework, pendingTask, 0);
        submission(fixture, passedTask, passedItem, 1, "FAILED", start.plusSeconds(1200));
        submission(fixture, passedTask, passedItem, 2, "PASSED", start.plusSeconds(1500));
        return new Facts(completedTopic, passedTask, passedItem, assessmentId);
    }

    private Fixture fixture() {
        UUID userId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID learningProgramId = UUID.randomUUID();
        UUID studentProgramId = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id, email) VALUES (?, ?)", userId, userId + "@example.com");
        jdbc.update(
                "INSERT INTO teachers(id, user_id, display_name) VALUES (?, ?, 'Teacher')",
                teacherId,
                userId);
        jdbc.update("INSERT INTO students(id, first_name) VALUES (?, 'Student')", studentId);
        jdbc.update(
                "INSERT INTO teacher_student_links(teacher_id, student_id) VALUES (?, ?)",
                teacherId,
                studentId);
        jdbc.update(
                "INSERT INTO subjects(id, owner_teacher_id, name) VALUES (?, ?, ?)",
                subjectId,
                teacherId,
                "Subject " + subjectId);
        jdbc.update(
                "INSERT INTO learning_programs(id, teacher_id, subject_id, title, status) VALUES (?, ?, ?, 'Program', 'ACTIVE')",
                learningProgramId,
                teacherId,
                subjectId);
        jdbc.update(
                "INSERT INTO student_programs(id, student_id, learning_program_id, assigned_by_teacher_id) VALUES (?, ?, ?, ?)",
                studentProgramId,
                studentId,
                learningProgramId,
                teacherId);
        return new Fixture(
                userId, teacherId, studentId, subjectId, learningProgramId, studentProgramId);
    }

    private UUID period(
            Fixture fixture,
            int sequence,
            int startMinutes,
            int target,
            Integer endMinutes,
            String status,
            Instant startedAt,
            Instant completedAt) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                """
            INSERT INTO learning_periods(
                id, student_program_id, sequence_no, start_cumulative_minutes,
                target_duration_minutes, end_cumulative_minutes, status, started_at, completed_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
                id,
                fixture.studentProgramId(),
                sequence,
                startMinutes,
                target,
                endMinutes,
                status,
                timestamp(startedAt),
                timestamp(completedAt));
        return id;
    }

    private UUID topic(
            Fixture fixture,
            UUID moduleId,
            int position,
            String title,
            String status,
            Instant startedAt,
            Instant completedAt) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO topics(id, module_id, title, position, status) VALUES (?, ?, ?, ?, 'ACTIVE')",
                id,
                moduleId,
                title,
                position);
        jdbc.update(
                "INSERT INTO student_topic_progress(student_program_id, topic_id, status, started_at, completed_at) VALUES (?, ?, ?, ?, ?)",
                fixture.studentProgramId(),
                id,
                status,
                timestamp(startedAt),
                timestamp(completedAt));
        return id;
    }

    private UUID session(Fixture fixture, Instant startedAt, int duration, String attendance) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO lesson_sessions(id, student_program_id, teacher_id, started_at, duration_minutes, attendance_status) VALUES (?, ?, ?, ?, ?, ?)",
                id,
                fixture.studentProgramId(),
                fixture.teacherId(),
                Timestamp.from(startedAt),
                duration,
                attendance);
        return id;
    }

    private UUID assessment(
            UUID sessionId,
            Integer understanding,
            Integer independence,
            Integer practice,
            Integer homework) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO teacher_assessments(id, lesson_session_id, understanding_score, independence_score, practice_score, homework_score) VALUES (?, ?, ?, ?, ?, ?)",
                id,
                sessionId,
                understanding,
                independence,
                practice,
                homework);
        return id;
    }

    private UUID homework(Fixture fixture, Instant assignedAt, String status, Instant completedAt) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO homeworks(id, student_program_id, assigned_by_teacher_id, title, assigned_at, status, completed_at) VALUES (?, ?, ?, 'Homework', ?, ?, ?)",
                id,
                fixture.studentProgramId(),
                fixture.teacherId(),
                Timestamp.from(assignedAt),
                status,
                timestamp(completedAt));
        return id;
    }

    private UUID task(Fixture fixture, String title) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO tasks(id, teacher_id, subject_id, title, description_markdown, task_type, status) VALUES (?, ?, ?, ?, 'Description', 'TEXT', 'ACTIVE')",
                id,
                fixture.teacherId(),
                fixture.subjectId(),
                title);
        return id;
    }

    private UUID item(UUID homeworkId, UUID taskId, int position) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO homework_items(id, homework_id, task_id, position) VALUES (?, ?, ?, ?)",
                id,
                homeworkId,
                taskId,
                position);
        return id;
    }

    private void submission(
            Fixture fixture,
            UUID taskId,
            UUID itemId,
            int attempt,
            String status,
            Instant submittedAt) {
        jdbc.update(
                "INSERT INTO submissions(id, student_id, student_program_id, task_id, homework_item_id, attempt_no, status, submitted_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(),
                fixture.studentId(),
                fixture.studentProgramId(),
                taskId,
                itemId,
                attempt,
                status,
                Timestamp.from(submittedAt));
    }

    private int sourceFactCount(UUID studentProgramId) {
        return jdbc.queryForObject(
                """
            SELECT (SELECT COUNT(*) FROM lesson_sessions WHERE student_program_id = ?)
                 + (SELECT COUNT(*) FROM homeworks WHERE student_program_id = ?)
                 + (SELECT COUNT(*) FROM submissions WHERE student_program_id = ?)
            """,
                Integer.class,
                studentProgramId,
                studentProgramId,
                studentProgramId);
    }

    private Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private record Fixture(
            UUID userId,
            UUID teacherId,
            UUID studentId,
            UUID subjectId,
            UUID learningProgramId,
            UUID studentProgramId) {
        AuthenticatedUser principal() {
            return new AuthenticatedUser(userId, "teacher@example.com", "", true, List.of());
        }
    }

    private record Facts(UUID completedTopicId, UUID taskId, UUID itemId, UUID assessmentId) {}
}
