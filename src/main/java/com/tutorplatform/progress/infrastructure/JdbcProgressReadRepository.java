package com.tutorplatform.progress.infrastructure;

import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressStatus;
import com.tutorplatform.progress.application.ProgressReadRepository;
import com.tutorplatform.progress.application.ProgressInterval;
import com.tutorplatform.progress.domain.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcProgressReadRepository implements ProgressReadRepository {

    private final JdbcClient jdbcClient;

    public JdbcProgressReadRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public SessionMetrics getSessionMetrics(UUID studentProgramId) {
        return jdbcClient.sql("""
                SELECT COALESCE(SUM(duration_minutes) FILTER (
                           WHERE attendance_status = 'ATTENDED'
                       ), 0) AS learning_minutes,
                       COUNT(*) FILTER (WHERE attendance_status = 'ATTENDED') AS attended_count,
                       COUNT(*) FILTER (WHERE attendance_status = 'MISSED') AS missed_count,
                       COUNT(*) FILTER (
                           WHERE attendance_status IN ('ATTENDED', 'MISSED')
                       ) AS sessions_count
                FROM lesson_sessions
                WHERE student_program_id = :studentProgramId
                """)
            .param("studentProgramId", studentProgramId)
            .query((resultSet, rowNumber) -> new SessionMetrics(
                resultSet.getLong("learning_minutes"),
                resultSet.getLong("attended_count"),
                resultSet.getLong("missed_count"),
                resultSet.getLong("sessions_count")
            ))
            .single();
    }

    @Override
    public List<TopicProgress> findTopicProgress(UUID studentProgramId) {
        return jdbcClient.sql("""
                SELECT topic.id AS topic_id, topic.title, progress.status
                FROM student_topic_progress progress
                JOIN student_programs student_program
                  ON student_program.id = progress.student_program_id
                JOIN topics topic ON topic.id = progress.topic_id
                JOIN modules module ON module.id = topic.module_id
                WHERE progress.student_program_id = :studentProgramId
                  AND module.learning_program_id = student_program.learning_program_id
                ORDER BY module.position, topic.position, topic.id
                """)
            .param("studentProgramId", studentProgramId)
            .query(JdbcProgressReadRepository::mapTopicProgress)
            .list();
    }

    @Override
    public HomeworkMetrics getHomeworkMetrics(UUID studentProgramId) {
        return jdbcClient.sql("""
                SELECT COUNT(*) FILTER (WHERE status <> 'CANCELLED') AS assigned_count,
                       COUNT(*) FILTER (WHERE status = 'COMPLETED') AS completed_count
                FROM homeworks
                WHERE student_program_id = :studentProgramId
                """)
            .param("studentProgramId", studentProgramId)
            .query((resultSet, rowNumber) -> new HomeworkMetrics(
                resultSet.getLong("assigned_count"),
                resultSet.getLong("completed_count")
            ))
            .single();
    }

    @Override
    public PracticeMetrics getPracticeMetrics(UUID studentProgramId) {
        return jdbcClient.sql("""
                SELECT COUNT(DISTINCT item.task_id) AS assigned_count,
                       COUNT(DISTINCT item.task_id) FILTER (
                           WHERE EXISTS (
                               SELECT 1
                               FROM submissions submission
                               WHERE submission.student_program_id = homework.student_program_id
                                 AND submission.homework_item_id = item.id
                                 AND submission.task_id = item.task_id
                                 AND submission.status = 'PASSED'
                           )
                       ) AS completed_count
                FROM homeworks homework
                JOIN homework_items item ON item.homework_id = homework.id
                WHERE homework.student_program_id = :studentProgramId
                  AND homework.status <> 'CANCELLED'
                """)
            .param("studentProgramId", studentProgramId)
            .query((resultSet, rowNumber) -> new PracticeMetrics(
                resultSet.getLong("assigned_count"),
                resultSet.getLong("completed_count")
            ))
            .single();
    }

    @Override
    public AssessmentAverages getAssessmentAverages(UUID studentProgramId) {
        return jdbcClient.sql("""
                SELECT AVG(assessment.understanding_score) AS understanding_average,
                       AVG(assessment.independence_score) AS independence_average,
                       AVG(assessment.practice_score) AS practice_average,
                       AVG(assessment.homework_score) AS homework_average
                FROM teacher_assessments assessment
                JOIN lesson_sessions session ON session.id = assessment.lesson_session_id
                WHERE session.student_program_id = :studentProgramId
                """)
            .param("studentProgramId", studentProgramId)
            .query((resultSet, rowNumber) -> new AssessmentAverages(
                resultSet.getBigDecimal("understanding_average"),
                resultSet.getBigDecimal("independence_average"),
                resultSet.getBigDecimal("practice_average"),
                resultSet.getBigDecimal("homework_average")
            ))
            .single();
    }

    @Override
    public SessionMetrics getSessionMetrics(UUID studentProgramId, ProgressInterval interval) {
        return jdbcClient.sql("""
                SELECT COALESCE(SUM(duration_minutes) FILTER (
                           WHERE attendance_status = 'ATTENDED'
                       ), 0) AS learning_minutes,
                       COUNT(*) FILTER (WHERE attendance_status = 'ATTENDED') AS attended_count,
                       COUNT(*) FILTER (WHERE attendance_status = 'MISSED') AS missed_count,
                       COUNT(*) FILTER (
                           WHERE attendance_status IN ('ATTENDED', 'MISSED')
                       ) AS sessions_count
                FROM lesson_sessions
                WHERE student_program_id = :studentProgramId
                  AND started_at >= :startedAt
                  AND started_at <= :endedAt
                """)
            .param("studentProgramId", studentProgramId)
            .param("startedAt", Timestamp.from(interval.startedAt()))
            .param("endedAt", Timestamp.from(interval.endedAt()))
            .query((resultSet, rowNumber) -> new SessionMetrics(
                resultSet.getLong("learning_minutes"),
                resultSet.getLong("attended_count"),
                resultSet.getLong("missed_count"),
                resultSet.getLong("sessions_count")
            ))
            .single();
    }

    @Override
    public List<TopicProgress> findTopicProgress(
        UUID studentProgramId,
        ProgressInterval interval
    ) {
        return jdbcClient.sql("""
                SELECT topic.id AS topic_id, topic.title,
                       CASE
                           WHEN progress.completed_at >= :startedAt
                            AND progress.completed_at <= :endedAt THEN 'COMPLETED'
                           ELSE 'IN_PROGRESS'
                       END AS status
                FROM student_topic_progress progress
                JOIN student_programs student_program
                  ON student_program.id = progress.student_program_id
                JOIN topics topic ON topic.id = progress.topic_id
                JOIN modules module ON module.id = topic.module_id
                WHERE progress.student_program_id = :studentProgramId
                  AND module.learning_program_id = student_program.learning_program_id
                  AND progress.started_at IS NOT NULL
                  AND progress.started_at <= :endedAt
                  AND (progress.completed_at IS NULL OR progress.completed_at >= :startedAt)
                ORDER BY module.position, topic.position, topic.id
                """)
            .param("studentProgramId", studentProgramId)
            .param("startedAt", Timestamp.from(interval.startedAt()))
            .param("endedAt", Timestamp.from(interval.endedAt()))
            .query(JdbcProgressReadRepository::mapTopicProgress)
            .list();
    }

    @Override
    public HomeworkMetrics getHomeworkMetrics(UUID studentProgramId, ProgressInterval interval) {
        return jdbcClient.sql("""
                SELECT COUNT(*) FILTER (
                           WHERE status <> 'CANCELLED'
                             AND assigned_at >= :startedAt
                             AND assigned_at <= :endedAt
                       ) AS assigned_count,
                       COUNT(*) FILTER (
                           WHERE status = 'COMPLETED'
                             AND completed_at >= :startedAt
                             AND completed_at <= :endedAt
                       ) AS completed_count
                FROM homeworks
                WHERE student_program_id = :studentProgramId
                """)
            .param("studentProgramId", studentProgramId)
            .param("startedAt", Timestamp.from(interval.startedAt()))
            .param("endedAt", Timestamp.from(interval.endedAt()))
            .query((resultSet, rowNumber) -> new HomeworkMetrics(
                resultSet.getLong("assigned_count"),
                resultSet.getLong("completed_count")
            ))
            .single();
    }

    @Override
    public PracticeMetrics getPracticeMetrics(UUID studentProgramId, ProgressInterval interval) {
        return jdbcClient.sql("""
                SELECT COUNT(DISTINCT item.task_id) AS assigned_count,
                       COUNT(DISTINCT item.task_id) FILTER (
                           WHERE EXISTS (
                               SELECT 1
                               FROM submissions submission
                               WHERE submission.student_program_id = homework.student_program_id
                                 AND submission.homework_item_id = item.id
                                 AND submission.task_id = item.task_id
                                 AND submission.status = 'PASSED'
                                 AND submission.submitted_at >= :startedAt
                                 AND submission.submitted_at <= :endedAt
                           )
                       ) AS completed_count
                FROM homeworks homework
                JOIN homework_items item ON item.homework_id = homework.id
                WHERE homework.student_program_id = :studentProgramId
                  AND homework.status <> 'CANCELLED'
                  AND homework.assigned_at >= :startedAt
                  AND homework.assigned_at <= :endedAt
                """)
            .param("studentProgramId", studentProgramId)
            .param("startedAt", Timestamp.from(interval.startedAt()))
            .param("endedAt", Timestamp.from(interval.endedAt()))
            .query((resultSet, rowNumber) -> new PracticeMetrics(
                resultSet.getLong("assigned_count"),
                resultSet.getLong("completed_count")
            ))
            .single();
    }

    @Override
    public AssessmentAverages getAssessmentAverages(
        UUID studentProgramId,
        ProgressInterval interval
    ) {
        return jdbcClient.sql("""
                SELECT AVG(assessment.understanding_score) AS understanding_average,
                       AVG(assessment.independence_score) AS independence_average,
                       AVG(assessment.practice_score) AS practice_average,
                       AVG(assessment.homework_score) AS homework_average
                FROM teacher_assessments assessment
                JOIN lesson_sessions session ON session.id = assessment.lesson_session_id
                WHERE session.student_program_id = :studentProgramId
                  AND session.started_at >= :startedAt
                  AND session.started_at <= :endedAt
                """)
            .param("studentProgramId", studentProgramId)
            .param("startedAt", Timestamp.from(interval.startedAt()))
            .param("endedAt", Timestamp.from(interval.endedAt()))
            .query((resultSet, rowNumber) -> new AssessmentAverages(
                resultSet.getBigDecimal("understanding_average"),
                resultSet.getBigDecimal("independence_average"),
                resultSet.getBigDecimal("practice_average"),
                resultSet.getBigDecimal("homework_average")
            ))
            .single();
    }

    private static TopicProgress mapTopicProgress(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TopicProgress(
            resultSet.getObject("topic_id", UUID.class),
            resultSet.getString("title"),
            StudentTopicProgressStatus.valueOf(resultSet.getString("status"))
        );
    }
}
