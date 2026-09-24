package com.tutorplatform.dashboard.infrastructure;

import com.tutorplatform.dashboard.application.TeacherDashboardAttentionType;
import com.tutorplatform.dashboard.application.TeacherDashboardQuery;
import com.tutorplatform.dashboard.application.TeacherDashboardResult;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTeacherDashboardQuery implements TeacherDashboardQuery {

    private static final String TEACHER_SCOPE =
            """
        current_teacher AS (
            SELECT teacher.id
            FROM teachers teacher
            WHERE teacher.user_id = :teacherUserId
        ),
        owned_students AS MATERIALIZED (
            SELECT DISTINCT link.student_id
            FROM current_teacher teacher
            JOIN teacher_student_links link ON link.teacher_id = teacher.id
            JOIN students student ON student.id = link.student_id
            WHERE link.ended_at IS NULL
              AND student.status = 'ACTIVE'
        )
        """;

    private static final String SUMMARY_SQL =
            "WITH "
                    + TEACHER_SCOPE
                    + """
        SELECT (SELECT count(*) FROM owned_students) AS active_students_count,
               (
                   SELECT count(*)
                   FROM submissions submission
                   JOIN owned_students owned ON owned.student_id = submission.student_id
                   WHERE submission.status = 'NEEDS_REVIEW'
               ) AS needs_review_submissions_count,
               (
                   SELECT count(*)
                   FROM homeworks homework
                   JOIN student_programs program ON program.id = homework.student_program_id
                   JOIN owned_students owned ON owned.student_id = program.student_id
                   WHERE homework.status = 'ASSIGNED'
                     AND homework.due_at IS NOT NULL
                     AND homework.due_at < now()
               ) AS overdue_homeworks_count,
               (
                   SELECT count(*)
                   FROM learning_periods period
                   JOIN student_programs program ON program.id = period.student_program_id
                   JOIN owned_students owned ON owned.student_id = program.student_id
                   WHERE period.status = 'COMPLETED'
                     AND NOT EXISTS (
                         SELECT 1
                         FROM progress_reports report
                         WHERE report.learning_period_id = period.id
                           AND report.status = 'PUBLISHED'
                     )
               ) AS completed_periods_without_published_report_count
        """;

    private static final String ATTENTION_SQL =
            "WITH "
                    + TEACHER_SCOPE
                    + """
        SELECT event_type,
               student_id,
               display_name,
               resource_id,
               event_at,
               student_program_id,
               homework_id,
               homework_item_id,
               task_id,
               submission_id,
               learning_period_id,
               report_id
        FROM (
            SELECT 'SUBMISSION_NEEDS_REVIEW' AS event_type,
                   student.id AS student_id,
                   concat_ws(' ', student.first_name, nullif(btrim(student.last_name), ''))
                       AS display_name,
                   submission.id AS resource_id,
                   submission.submitted_at AS event_at,
                   submission.student_program_id,
                   homework.id AS homework_id,
                   submission.homework_item_id,
                   submission.task_id,
                   submission.id AS submission_id,
                   NULL::uuid AS learning_period_id,
                   NULL::uuid AS report_id
            FROM submissions submission
            JOIN owned_students owned ON owned.student_id = submission.student_id
            JOIN students student ON student.id = submission.student_id
            LEFT JOIN homework_items item ON item.id = submission.homework_item_id
            LEFT JOIN homeworks homework ON homework.id = item.homework_id
            WHERE submission.status = 'NEEDS_REVIEW'

            UNION ALL

            SELECT 'HOMEWORK_OVERDUE' AS event_type,
                   student.id AS student_id,
                   concat_ws(' ', student.first_name, nullif(btrim(student.last_name), ''))
                       AS display_name,
                   homework.id AS resource_id,
                   homework.due_at AS event_at,
                   homework.student_program_id,
                   homework.id AS homework_id,
                   NULL::uuid AS homework_item_id,
                   NULL::uuid AS task_id,
                   NULL::uuid AS submission_id,
                   NULL::uuid AS learning_period_id,
                   NULL::uuid AS report_id
            FROM homeworks homework
            JOIN student_programs program ON program.id = homework.student_program_id
            JOIN owned_students owned ON owned.student_id = program.student_id
            JOIN students student ON student.id = program.student_id
            WHERE homework.status = 'ASSIGNED'
              AND homework.due_at IS NOT NULL
              AND homework.due_at < now()

            UNION ALL

            SELECT 'LEARNING_PERIOD_REPORT_MISSING' AS event_type,
                   student.id AS student_id,
                   concat_ws(' ', student.first_name, nullif(btrim(student.last_name), ''))
                       AS display_name,
                   period.id AS resource_id,
                   period.completed_at AS event_at,
                   period.student_program_id,
                   NULL::uuid AS homework_id,
                   NULL::uuid AS homework_item_id,
                   NULL::uuid AS task_id,
                   NULL::uuid AS submission_id,
                   period.id AS learning_period_id,
                   report.id AS report_id
            FROM learning_periods period
            JOIN student_programs program ON program.id = period.student_program_id
            JOIN owned_students owned ON owned.student_id = program.student_id
            JOIN students student ON student.id = program.student_id
            LEFT JOIN progress_reports report ON report.learning_period_id = period.id
            WHERE period.status = 'COMPLETED'
              AND NOT EXISTS (
                  SELECT 1
                  FROM progress_reports published_report
                  WHERE published_report.learning_period_id = period.id
                    AND published_report.status = 'PUBLISHED'
              )
        ) attention
        ORDER BY event_at DESC, event_type ASC, resource_id ASC
        LIMIT :limit
        """;

    private final JdbcClient jdbcClient;

    public JdbcTeacherDashboardQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Summary getSummary(UUID teacherUserId) {
        return jdbcClient
                .sql(SUMMARY_SQL)
                .param("teacherUserId", teacherUserId)
                .query(JdbcTeacherDashboardQuery::mapSummary)
                .single();
    }

    @Override
    public List<TeacherDashboardResult.AttentionItem> findAttentionItems(
            UUID teacherUserId, int limit) {
        return jdbcClient
                .sql(ATTENTION_SQL)
                .param("teacherUserId", teacherUserId)
                .param("limit", limit)
                .query(JdbcTeacherDashboardQuery::mapAttentionItem)
                .list();
    }

    private static Summary mapSummary(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Summary(
                resultSet.getLong("active_students_count"),
                resultSet.getLong("needs_review_submissions_count"),
                resultSet.getLong("overdue_homeworks_count"),
                resultSet.getLong("completed_periods_without_published_report_count"));
    }

    private static TeacherDashboardResult.AttentionItem mapAttentionItem(
            ResultSet resultSet, int rowNumber) throws SQLException {
        return new TeacherDashboardResult.AttentionItem(
                TeacherDashboardAttentionType.valueOf(resultSet.getString("event_type")),
                resultSet.getObject("student_id", UUID.class),
                resultSet.getString("display_name"),
                resultSet.getObject("resource_id", UUID.class),
                resultSet.getTimestamp("event_at").toInstant(),
                new TeacherDashboardResult.Navigation(
                        resultSet.getObject("student_program_id", UUID.class),
                        resultSet.getObject("homework_id", UUID.class),
                        resultSet.getObject("homework_item_id", UUID.class),
                        resultSet.getObject("task_id", UUID.class),
                        resultSet.getObject("submission_id", UUID.class),
                        resultSet.getObject("learning_period_id", UUID.class),
                        resultSet.getObject("report_id", UUID.class)));
    }
}
