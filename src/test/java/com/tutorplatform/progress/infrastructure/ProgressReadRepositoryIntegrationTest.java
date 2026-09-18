package com.tutorplatform.progress.infrastructure;

import com.tutorplatform.test.PostgresIntegrationTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.tutorplatform.progress.application.GetCurrentProgressService;
import com.tutorplatform.progress.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class ProgressReadRepositoryIntegrationTest extends PostgresIntegrationTest {

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_progress_read_repository", "008");
    }

    @Autowired
    private JdbcProgressReadRepository repository;
    @Autowired
    private GetCurrentProgressService service;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void sessionMetricsSumOnlyAttendedAndExcludeCancelledFromRelevantCount() {
        Fixture fixture = fixture();
        session(fixture, "ATTENDED", 60);
        session(fixture, "ATTENDED", 90);
        session(fixture, "MISSED", 45);
        session(fixture, "CANCELLED", 30);

        SessionMetrics metrics = repository.getSessionMetrics(fixture.studentProgramId());

        assertThat(metrics.totalLearningMinutes()).isEqualTo(150);
        assertThat(metrics.attendedCount()).isEqualTo(2);
        assertThat(metrics.missedCount()).isOne();
        assertThat(metrics.sessionsCount()).isEqualTo(3);
    }

    @Test
    void sessionMetricsAreZeroWhenNoSessionsExist() {
        SessionMetrics metrics = repository.getSessionMetrics(fixture().studentProgramId());

        assertThat(metrics).isEqualTo(new SessionMetrics(0, 0, 0, 0));
    }

    @Test
    void topicProgressIsScopedToStudentProgramAndOrderedByModuleThenTopic() {
        Fixture fixture = fixture();
        Fixture other = fixture();
        UUID laterModule = module(fixture, 1);
        UUID firstModule = module(fixture, 0);
        UUID secondTopic = topic(fixture, firstModule, 1, "Second", "IN_PROGRESS");
        UUID thirdTopic = topic(fixture, laterModule, 0, "Third", "AVAILABLE");
        UUID firstTopic = topic(fixture, firstModule, 0, "First", "COMPLETED");
        UUID lockedTopic = topic(other, module(other, 0), 0, "Other", "LOCKED");

        assertThat(repository.findTopicProgress(fixture.studentProgramId()))
            .extracting(TopicProgress::topicId)
            .containsExactly(firstTopic, secondTopic, thirdTopic)
            .doesNotContain(lockedTopic);
    }

    @Test
    void homeworkMetricsCountAssignedAndCompletedButExcludeCancelledAndOtherProgram() {
        Fixture fixture = fixture();
        Fixture other = fixture();
        homework(fixture, "ASSIGNED");
        homework(fixture, "COMPLETED");
        homework(fixture, "CANCELLED");
        homework(other, "COMPLETED");

        assertThat(repository.getHomeworkMetrics(fixture.studentProgramId()))
            .isEqualTo(new HomeworkMetrics(2, 1));
    }

    @Test
    void practiceCountsDistinctAssignedTasksWithAtLeastOnePassedHomeworkSubmission() {
        Fixture fixture = fixture();
        UUID homeworkId = homework(fixture, "ASSIGNED");
        UUID passedTask = task(fixture, "Passed task");
        UUID failedTask = task(fixture, "Failed task");
        UUID reviewTask = task(fixture, "Review task");
        UUID systemErrorTask = task(fixture, "System error task");
        UUID passedItem = item(homeworkId, passedTask, 0);
        UUID failedItem = item(homeworkId, failedTask, 1);
        UUID reviewItem = item(homeworkId, reviewTask, 2);
        UUID systemErrorItem = item(homeworkId, systemErrorTask, 3);
        submission(fixture, passedTask, passedItem, 1, "FAILED");
        submission(fixture, passedTask, passedItem, 2, "PASSED");
        submission(fixture, passedTask, passedItem, 3, "PASSED");
        submission(fixture, failedTask, failedItem, 1, "FAILED");
        submission(fixture, reviewTask, reviewItem, 1, "NEEDS_REVIEW");
        submission(fixture, systemErrorTask, systemErrorItem, 1, "SYSTEM_ERROR");

        assertThat(repository.getPracticeMetrics(fixture.studentProgramId()))
            .isEqualTo(new PracticeMetrics(4, 1));
    }

    @Test
    void practiceUsesDistinctTaskGranularityAcrossHomeworkAssignments() {
        Fixture fixture = fixture();
        UUID taskId = task(fixture, "Repeated task");
        UUID firstItem = item(homework(fixture, "ASSIGNED"), taskId, 0);
        item(homework(fixture, "COMPLETED"), taskId, 0);
        submission(fixture, taskId, firstItem, 1, "PASSED");

        assertThat(repository.getPracticeMetrics(fixture.studentProgramId()))
            .isEqualTo(new PracticeMetrics(1, 1));
    }

    @Test
    void practiceIgnoresFreePracticeForeignTasksAndCancelledHomework() {
        Fixture fixture = fixture();
        Fixture other = fixture();
        UUID assignedTask = task(fixture, "Assigned");
        item(homework(fixture, "ASSIGNED"), assignedTask, 0);
        UUID freeTask = task(fixture, "Free practice");
        submission(fixture, freeTask, null, 1, "PASSED");
        UUID cancelledTask = task(fixture, "Cancelled");
        UUID cancelledItem = item(homework(fixture, "CANCELLED"), cancelledTask, 0);
        submission(fixture, cancelledTask, cancelledItem, 1, "PASSED");
        UUID otherTask = task(other, "Other program");
        UUID otherItem = item(homework(other, "ASSIGNED"), otherTask, 0);
        submission(other, otherTask, otherItem, 1, "PASSED");

        assertThat(repository.getPracticeMetrics(fixture.studentProgramId()))
            .isEqualTo(new PracticeMetrics(1, 0));
    }

    @Test
    void assessmentAveragesIgnoreNullsAndCalculateCategoriesIndependently() {
        Fixture fixture = fixture();
        assessment(session(fixture, "ATTENDED", 60), 5, 2, null, 5);
        assessment(session(fixture, "ATTENDED", 60), null, 4, 3, 3);
        assessment(session(fixture, "MISSED", 60), 3, null, 5, null);

        AssessmentAverages averages = repository.getAssessmentAverages(fixture.studentProgramId());

        assertDecimal(averages.understanding(), "4.0");
        assertDecimal(averages.independence(), "3.0");
        assertDecimal(averages.practice(), "4.0");
        assertDecimal(averages.homework(), "4.0");
    }

    @Test
    void assessmentAveragesAreNullWithoutScoresAndIgnoreOtherPrograms() {
        Fixture fixture = fixture();
        assessment(session(fixture, "ATTENDED", 60), null, null, null, null);
        Fixture other = fixture();
        assessment(session(other, "ATTENDED", 60), 5, 5, 5, 5);

        assertThat(repository.getAssessmentAverages(fixture.studentProgramId()))
            .isEqualTo(new AssessmentAverages(null, null, null, null));
    }

    @Test
    void serviceBuildsConsistentCurrentProgressFromAllFacts() {
        Fixture fixture = fixture();
        UUID firstModule = module(fixture, 0);
        UUID completedTopic = topic(fixture, firstModule, 0, "Completed", "COMPLETED");
        UUID inProgressTopic = topic(fixture, firstModule, 1, "In progress", "IN_PROGRESS");
        UUID firstSession = session(fixture, "ATTENDED", 60);
        UUID secondSession = session(fixture, "ATTENDED", 60);
        session(fixture, "MISSED", 30);
        assessment(firstSession, 5, 4, null, 3);
        assessment(secondSession, 3, null, 5, 5);
        UUID completedHomework = homework(fixture, "COMPLETED");
        UUID assignedHomework = homework(fixture, "ASSIGNED");
        UUID passedTask = task(fixture, "Passed");
        UUID failedTask = task(fixture, "Failed");
        UUID passedItem = item(completedHomework, passedTask, 0);
        UUID failedItem = item(assignedHomework, failedTask, 0);
        submission(fixture, passedTask, passedItem, 1, "FAILED");
        submission(fixture, passedTask, passedItem, 2, "PASSED");
        submission(fixture, failedTask, failedItem, 1, "FAILED");

        CurrentProgress progress = service.getCurrentProgress(fixture.studentProgramId());

        assertThat(progress.studentProgramId()).isEqualTo(fixture.studentProgramId());
        assertThat(progress.totalLearningMinutes()).isEqualTo(120);
        assertThat(progress.sessionsCount()).isEqualTo(3);
        assertThat(progress.attendanceRate()).isCloseTo(2.0 / 3.0, within(1.0e-12));
        assertThat(progress.totalTopics()).isEqualTo(2);
        assertThat(progress.completedTopics()).extracting(TopicProgress::topicId)
            .containsExactly(completedTopic);
        assertThat(progress.inProgressTopics()).extracting(TopicProgress::topicId)
            .containsExactly(inProgressTopic);
        assertThat(progress.homeworkAssigned()).isEqualTo(2);
        assertThat(progress.homeworkCompleted()).isOne();
        assertThat(progress.practiceAssigned()).isEqualTo(2);
        assertThat(progress.practiceCompleted()).isOne();
        assertDecimal(progress.assessmentAverages().understanding(), "4.0");
        assertDecimal(progress.assessmentAverages().independence(), "4.0");
        assertDecimal(progress.assessmentAverages().practice(), "5.0");
        assertDecimal(progress.assessmentAverages().homework(), "4.0");
    }

    @Test
    void serviceReturnsSafeEmptyState() {
        CurrentProgress progress = service.getCurrentProgress(fixture().studentProgramId());

        assertThat(progress.totalLearningMinutes()).isZero();
        assertThat(progress.sessionsCount()).isZero();
        assertThat(progress.attendanceRate()).isZero();
        assertThat(progress.totalTopics()).isZero();
        assertThat(progress.completedTopics()).isEmpty();
        assertThat(progress.inProgressTopics()).isEmpty();
        assertThat(progress.homeworkAssigned()).isZero();
        assertThat(progress.homeworkCompleted()).isZero();
        assertThat(progress.practiceAssigned()).isZero();
        assertThat(progress.practiceCompleted()).isZero();
        assertThat(progress.assessmentAverages())
            .isEqualTo(new AssessmentAverages(null, null, null, null));
    }

    @Test
    void serviceDoesNotModifyOperationalFacts() {
        Fixture fixture = fixture();
        UUID moduleId = module(fixture, 0);
        topic(fixture, moduleId, 0, "Topic", "IN_PROGRESS");
        UUID sessionId = session(fixture, "ATTENDED", 60);
        assessment(sessionId, 4, null, null, null);
        UUID homeworkId = homework(fixture, "ASSIGNED");
        UUID taskId = task(fixture, "Task");
        UUID itemId = item(homeworkId, taskId, 0);
        submission(fixture, taskId, itemId, 1, "PASSED");
        int factsBefore = factCount(fixture.studentProgramId());

        service.getCurrentProgress(fixture.studentProgramId());

        assertThat(factCount(fixture.studentProgramId())).isEqualTo(factsBefore);
        assertThat(jdbc.queryForObject(
            "SELECT status FROM student_topic_progress WHERE student_program_id = ?",
            String.class,
            fixture.studentProgramId()
        )).isEqualTo("IN_PROGRESS");
        assertThat(jdbc.queryForObject(
            "SELECT status FROM homeworks WHERE id = ?",
            String.class,
            homeworkId
        )).isEqualTo("ASSIGNED");
        assertThat(jdbc.queryForObject(
            "SELECT status FROM submissions WHERE homework_item_id = ?",
            String.class,
            itemId
        )).isEqualTo("PASSED");
    }

    private Fixture fixture() {
        UUID userId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID learningProgramId = UUID.randomUUID();
        UUID studentProgramId = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id, email) VALUES (?, ?)", userId, userId + "@example.com");
        jdbc.update("INSERT INTO teachers(id, user_id, display_name) VALUES (?, ?, 'Teacher')", teacherId, userId);
        jdbc.update("INSERT INTO students(id, first_name) VALUES (?, 'Student')", studentId);
        jdbc.update("INSERT INTO subjects(id, owner_teacher_id, name) VALUES (?, ?, ?)", subjectId, teacherId, "Subject " + subjectId);
        jdbc.update("INSERT INTO learning_programs(id, teacher_id, subject_id, title, status) VALUES (?, ?, ?, 'Program', 'ACTIVE')", learningProgramId, teacherId, subjectId);
        jdbc.update("INSERT INTO student_programs(id, student_id, learning_program_id, assigned_by_teacher_id) VALUES (?, ?, ?, ?)", studentProgramId, studentId, learningProgramId, teacherId);
        return new Fixture(teacherId, studentId, subjectId, learningProgramId, studentProgramId);
    }

    private UUID module(Fixture fixture, int position) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO modules(id, learning_program_id, title, position) VALUES (?, ?, ?, ?)", id, fixture.learningProgramId(), "Module " + id, position);
        return id;
    }

    private UUID topic(Fixture fixture, UUID moduleId, int position, String title, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO topics(id, module_id, title, position, status) VALUES (?, ?, ?, ?, 'ACTIVE')", id, moduleId, title, position);
        jdbc.update("INSERT INTO student_topic_progress(student_program_id, topic_id, status) VALUES (?, ?, ?)", fixture.studentProgramId(), id, status);
        return id;
    }

    private UUID session(Fixture fixture, String attendanceStatus, int durationMinutes) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO lesson_sessions(id, student_program_id, teacher_id, started_at, duration_minutes, attendance_status) VALUES (?, ?, ?, now(), ?, ?)", id, fixture.studentProgramId(), fixture.teacherId(), durationMinutes, attendanceStatus);
        return id;
    }

    private UUID homework(Fixture fixture, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO homeworks(id, student_program_id, assigned_by_teacher_id, title, status, completed_at) VALUES (?, ?, ?, 'Homework', ?, CASE WHEN ? = 'COMPLETED' THEN now() END)", id, fixture.studentProgramId(), fixture.teacherId(), status, status);
        return id;
    }

    private UUID task(Fixture fixture, String title) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO tasks(id, teacher_id, subject_id, title, description_markdown, task_type, status) VALUES (?, ?, ?, ?, 'Description', 'TEXT', 'ACTIVE')", id, fixture.teacherId(), fixture.subjectId(), title);
        return id;
    }

    private UUID item(UUID homeworkId, UUID taskId, int position) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO homework_items(id, homework_id, task_id, position) VALUES (?, ?, ?, ?)", id, homeworkId, taskId, position);
        return id;
    }

    private void submission(Fixture fixture, UUID taskId, UUID homeworkItemId, int attempt, String status) {
        jdbc.update("INSERT INTO submissions(id, student_id, student_program_id, task_id, homework_item_id, attempt_no, status) VALUES (?, ?, ?, ?, ?, ?, ?)", UUID.randomUUID(), fixture.studentId(), fixture.studentProgramId(), taskId, homeworkItemId, attempt, status);
    }

    private void assessment(UUID sessionId, Integer understanding, Integer independence, Integer practice, Integer homework) {
        jdbc.update("INSERT INTO teacher_assessments(id, lesson_session_id, understanding_score, independence_score, practice_score, homework_score) VALUES (?, ?, ?, ?, ?, ?)", UUID.randomUUID(), sessionId, understanding, independence, practice, homework);
    }

    private int factCount(UUID studentProgramId) {
        return jdbc.queryForObject("""
            SELECT
                (SELECT COUNT(*) FROM lesson_sessions WHERE student_program_id = ?) +
                (SELECT COUNT(*) FROM student_topic_progress WHERE student_program_id = ?) +
                (SELECT COUNT(*) FROM homeworks WHERE student_program_id = ?) +
                (SELECT COUNT(*) FROM submissions WHERE student_program_id = ?) +
                (SELECT COUNT(*)
                   FROM teacher_assessments assessment
                   JOIN lesson_sessions session ON session.id = assessment.lesson_session_id
                  WHERE session.student_program_id = ?)
            """, Integer.class,
            studentProgramId, studentProgramId, studentProgramId, studentProgramId, studentProgramId
        );
    }

    private static void assertDecimal(BigDecimal actual, String expected) {
        assertThat(actual).isEqualByComparingTo(expected);
    }

    private static org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }

    private record Fixture(
        UUID teacherId,
        UUID studentId,
        UUID subjectId,
        UUID learningProgramId,
        UUID studentProgramId
    ) {
    }
}
