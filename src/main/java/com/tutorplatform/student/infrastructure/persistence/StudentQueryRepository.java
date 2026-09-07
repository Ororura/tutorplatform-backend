package com.tutorplatform.student.infrastructure.persistence;

import com.tutorplatform.student.domain.StudentAccountStatus;
import com.tutorplatform.student.domain.StudentStatus;
import com.tutorplatform.student.domain.TeacherStudentRelationType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class StudentQueryRepository {

    private static final String ACCOUNT_STATUS_SQL = """
        CASE
            WHEN s.user_id IS NOT NULL THEN 'REGISTERED'
            WHEN EXISTS (
                SELECT 1
                FROM student_invites si
                WHERE si.student_id = s.id
                  AND si.accepted_at IS NULL
                  AND si.revoked_at IS NULL
                  AND si.expires_at > now()
            ) THEN 'INVITED'
            ELSE 'UNREGISTERED'
        END
        """;

    private final JdbcClient jdbcClient;

    public StudentQueryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public StudentPage findStudents(UUID teacherId, int page, int size, String searchPattern, StudentAccountStatus accountStatus, String sortField, boolean ascending) {
        String filters = ("""
            FROM teacher_student_links link
            JOIN students s ON s.id = link.student_id
            WHERE link.teacher_id = :teacherId
              AND link.relation_type = 'PRIMARY'
              AND link.ended_at IS NULL
              AND (:searchPattern IS NULL
                   OR lower(s.first_name) LIKE :searchPattern ESCAPE '\\'
                   OR lower(COALESCE(s.last_name, '')) LIKE :searchPattern ESCAPE '\\')
              AND (:accountStatus IS NULL OR (%s) = :accountStatus)
            """).formatted(ACCOUNT_STATUS_SQL);

        String selectSql = ("""
            SELECT s.id,
                   s.first_name,
                   s.last_name,
                   s.status,
                   %s AS account_status,
                   s.created_at
            %s
            ORDER BY
                CASE WHEN :sortField = 'createdAt' AND :ascending THEN s.created_at END ASC NULLS LAST,
                CASE WHEN :sortField = 'createdAt' AND NOT :ascending THEN s.created_at END DESC NULLS LAST,
                CASE WHEN :sortField = 'firstName' AND :ascending THEN lower(s.first_name) END ASC NULLS LAST,
                CASE WHEN :sortField = 'firstName' AND NOT :ascending THEN lower(s.first_name) END DESC NULLS LAST,
                CASE WHEN :sortField = 'lastName' AND :ascending THEN lower(s.last_name) END ASC NULLS LAST,
                CASE WHEN :sortField = 'lastName' AND NOT :ascending THEN lower(s.last_name) END DESC NULLS LAST,
                s.id ASC
            LIMIT :size OFFSET :offset
            """).formatted(ACCOUNT_STATUS_SQL, filters);

        long totalElements = statement("SELECT count(*) " + filters, teacherId, searchPattern, accountStatus).query(Long.class).single();

        List<StudentSummaryRow> items = statement(selectSql, teacherId, searchPattern, accountStatus).param("sortField", sortField).param("ascending", ascending).param("size", size).param("offset", (long) page * size).query(StudentQueryRepository::mapSummary).list();

        return new StudentPage(items, totalElements);
    }

    public Optional<StudentDetailsRow> findDetails(UUID teacherId, UUID studentId) {
        String sql = ("""
            SELECT s.id,
                   s.first_name,
                   s.last_name,
                   s.status,
                   %s AS account_status,
                   COALESCE(u.email::text, active_invite.email::text) AS account_email,
                   link.relation_type,
                   link.started_at,
                   s.created_at,
                   s.updated_at
            FROM teacher_student_links link
            JOIN students s ON s.id = link.student_id
            LEFT JOIN users u ON u.id = s.user_id
            LEFT JOIN LATERAL (
                SELECT si.email
                FROM student_invites si
                WHERE si.student_id = s.id
                  AND si.accepted_at IS NULL
                  AND si.revoked_at IS NULL
                  AND si.expires_at > now()
                ORDER BY si.created_at DESC
                LIMIT 1
            ) active_invite ON true
            WHERE link.teacher_id = :teacherId
              AND link.student_id = :studentId
              AND link.relation_type = 'PRIMARY'
              AND link.ended_at IS NULL
            """).formatted(ACCOUNT_STATUS_SQL);

        return jdbcClient.sql(sql).param("teacherId", teacherId).param("studentId", studentId).query(StudentQueryRepository::mapDetails).optional();
    }

    private JdbcClient.StatementSpec statement(String sql, UUID teacherId, String searchPattern, StudentAccountStatus accountStatus) {
        return jdbcClient.sql(sql).param("teacherId", teacherId).param("searchPattern", searchPattern, Types.VARCHAR).param("accountStatus", accountStatus == null ? null : accountStatus.name(), Types.VARCHAR);
    }

    private static StudentSummaryRow mapSummary(ResultSet resultSet, int rowNumber) throws SQLException {
        return new StudentSummaryRow(resultSet.getObject("id", UUID.class), resultSet.getString("first_name"), resultSet.getString("last_name"), StudentStatus.valueOf(resultSet.getString("status")), StudentAccountStatus.valueOf(resultSet.getString("account_status")), resultSet.getTimestamp("created_at").toInstant());
    }

    private static StudentDetailsRow mapDetails(ResultSet resultSet, int rowNumber) throws SQLException {
        return new StudentDetailsRow(resultSet.getObject("id", UUID.class), resultSet.getString("first_name"), resultSet.getString("last_name"), StudentStatus.valueOf(resultSet.getString("status")), StudentAccountStatus.valueOf(resultSet.getString("account_status")), resultSet.getString("account_email"), TeacherStudentRelationType.valueOf(resultSet.getString("relation_type")), resultSet.getTimestamp("started_at").toInstant(), resultSet.getTimestamp("created_at").toInstant(), resultSet.getTimestamp("updated_at").toInstant());
    }

    public record StudentPage(List<StudentSummaryRow> items, long totalElements) {
    }

    public record StudentSummaryRow(UUID id, String firstName, String lastName, StudentStatus status,
                                    StudentAccountStatus accountStatus, Instant createdAt) {
    }

    public record StudentDetailsRow(UUID id, String firstName, String lastName, StudentStatus status,
                                    StudentAccountStatus accountStatus, String accountEmail,
                                    TeacherStudentRelationType relationType, Instant relationStartedAt,
                                    Instant createdAt, Instant updatedAt) {
    }
}
