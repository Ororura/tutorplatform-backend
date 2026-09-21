package com.tutorplatform.program.infrastructure.read;

import com.tutorplatform.program.application.TeacherStudentProgramQuery;
import com.tutorplatform.program.domain.TopicStatus;
import com.tutorplatform.program.domain.studentprogram.StudentProgramStatus;
import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTeacherStudentProgramQuery implements TeacherStudentProgramQuery {

    private static final String HEADER_COLUMNS =
            """
        sp.id,
        sp.learning_program_id,
        lp.title,
        lp.description,
        sp.status,
        sp.report_interval_minutes,
        sp.started_at,
        sp.completed_at,
        subject.id AS subject_id,
        subject.code AS subject_code,
        subject.name AS subject_name
        """;

    private static final String OWNED_PROGRAM_FROM =
            """
        FROM teacher_student_links link
        JOIN student_programs sp ON sp.student_id = link.student_id
        JOIN learning_programs lp ON lp.id = sp.learning_program_id
        JOIN subjects subject ON subject.id = lp.subject_id
        WHERE link.teacher_id = :teacherId
          AND link.student_id = :studentId
          AND link.relation_type = 'PRIMARY'
          AND link.ended_at IS NULL
          AND sp.assigned_by_teacher_id = :teacherId
          AND lp.teacher_id = :teacherId
        """;

    private static final String STUDENT_PROGRAM_FROM =
            """
        FROM student_programs sp
        JOIN learning_programs lp ON lp.id = sp.learning_program_id
        JOIN subjects subject ON subject.id = lp.subject_id
        WHERE sp.student_id = :studentId
        """;

    private final JdbcClient jdbcClient;

    public JdbcTeacherStudentProgramQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<StudentProgramSummary> findPrograms(UUID teacherId, UUID studentId) {
        String sql =
                "SELECT "
                        + HEADER_COLUMNS
                        + OWNED_PROGRAM_FROM
                        + """
            ORDER BY sp.started_at DESC, sp.id ASC
            """;
        return ownedStatement(sql, teacherId, studentId)
                .query(JdbcTeacherStudentProgramQuery::mapSummary)
                .list();
    }

    @Override
    public Optional<StudentProgramDetails> findProgram(
            UUID teacherId, UUID studentId, UUID studentProgramId) {
        String headerSql =
                "SELECT "
                        + HEADER_COLUMNS
                        + OWNED_PROGRAM_FROM
                        + """
              AND sp.id = :studentProgramId
            """;
        Optional<StudentProgramSummary> header =
                ownedStatement(headerSql, teacherId, studentId)
                        .param("studentProgramId", studentProgramId)
                        .query(JdbcTeacherStudentProgramQuery::mapSummary)
                        .optional();
        return details(header, studentProgramId);
    }

    @Override
    public List<StudentProgramSummary> findProgramsByStudentId(UUID studentId) {
        String sql =
                "SELECT "
                        + HEADER_COLUMNS
                        + STUDENT_PROGRAM_FROM
                        + """
            ORDER BY sp.started_at DESC, sp.id ASC
            """;
        return jdbcClient
                .sql(sql)
                .param("studentId", studentId)
                .query(JdbcTeacherStudentProgramQuery::mapSummary)
                .list();
    }

    @Override
    public Optional<StudentProgramDetails> findProgramByStudentId(
            UUID studentId, UUID studentProgramId) {
        String sql =
                "SELECT "
                        + HEADER_COLUMNS
                        + STUDENT_PROGRAM_FROM
                        + """
              AND sp.id = :studentProgramId
            """;
        Optional<StudentProgramSummary> header =
                jdbcClient
                        .sql(sql)
                        .param("studentId", studentId)
                        .param("studentProgramId", studentProgramId)
                        .query(JdbcTeacherStudentProgramQuery::mapSummary)
                        .optional();
        return details(header, studentProgramId);
    }

    private Optional<StudentProgramDetails> details(
            Optional<StudentProgramSummary> header, UUID studentProgramId) {
        if (header.isEmpty()) {
            return Optional.empty();
        }

        List<StructureRow> rows =
                jdbcClient
                        .sql(
                                """
                SELECT m.id AS module_id,
                       m.title AS module_title,
                       m.description AS module_description,
                       m.position AS module_position,
                       t.id AS topic_id,
                       t.title AS topic_title,
                       t.description AS topic_description,
                       t.position AS topic_position,
                       t.status AS topic_status,
                       progress.status AS progress_status
                FROM modules m
                LEFT JOIN topics t ON t.module_id = m.id
                LEFT JOIN student_topic_progress progress
                       ON progress.student_program_id = :studentProgramId
                      AND progress.topic_id = t.id
                WHERE m.learning_program_id = :learningProgramId
                ORDER BY m.position ASC, m.id ASC, t.position ASC, t.id ASC
                """)
                        .param("studentProgramId", studentProgramId)
                        .param("learningProgramId", header.orElseThrow().learningProgramId())
                        .query(JdbcTeacherStudentProgramQuery::mapStructure)
                        .list();

        StudentProgramSummary value = header.orElseThrow();
        return Optional.of(
                new StudentProgramDetails(
                        value.id(),
                        value.learningProgramId(),
                        value.title(),
                        value.description(),
                        value.status(),
                        value.reportIntervalMinutes(),
                        value.startedAt(),
                        value.completedAt(),
                        value.subject(),
                        assembleModules(rows)));
    }

    private JdbcClient.StatementSpec ownedStatement(String sql, UUID teacherId, UUID studentId) {
        return jdbcClient.sql(sql).param("teacherId", teacherId).param("studentId", studentId);
    }

    private static StudentProgramSummary mapSummary(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new StudentProgramSummary(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("learning_program_id", UUID.class),
                resultSet.getString("title"),
                resultSet.getString("description"),
                StudentProgramStatus.valueOf(resultSet.getString("status")),
                resultSet.getInt("report_interval_minutes"),
                resultSet.getTimestamp("started_at").toInstant(),
                instant(resultSet.getTimestamp("completed_at")),
                new ProgramSubject(
                        resultSet.getObject("subject_id", UUID.class),
                        resultSet.getString("subject_code"),
                        resultSet.getString("subject_name")));
    }

    private static StructureRow mapStructure(ResultSet resultSet, int rowNumber)
            throws SQLException {
        UUID topicId = resultSet.getObject("topic_id", UUID.class);
        String progressStatus = resultSet.getString("progress_status");
        return new StructureRow(
                resultSet.getObject("module_id", UUID.class),
                resultSet.getString("module_title"),
                resultSet.getString("module_description"),
                resultSet.getInt("module_position"),
                topicId,
                topicId == null ? null : resultSet.getString("topic_title"),
                topicId == null ? null : resultSet.getString("topic_description"),
                topicId == null ? null : resultSet.getInt("topic_position"),
                topicId == null ? null : TopicStatus.valueOf(resultSet.getString("topic_status")),
                progressStatus == null ? null : StudentTopicProgressStatus.valueOf(progressStatus));
    }

    private static List<ProgramModule> assembleModules(List<StructureRow> rows) {
        LinkedHashMap<UUID, MutableModule> modules = new LinkedHashMap<>();
        for (StructureRow row : rows) {
            MutableModule module =
                    modules.computeIfAbsent(
                            row.moduleId(),
                            ignored ->
                                    new MutableModule(
                                            row.moduleId(),
                                            row.moduleTitle(),
                                            row.moduleDescription(),
                                            row.modulePosition()));
            if (row.topicId() != null) {
                module.topics.add(
                        new ProgramTopic(
                                row.topicId(),
                                row.topicTitle(),
                                row.topicDescription(),
                                row.topicPosition(),
                                row.topicStatus(),
                                row.progressStatus()));
            }
        }
        return modules.values().stream().map(MutableModule::toProgramModule).toList();
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private record StructureRow(
            UUID moduleId,
            String moduleTitle,
            String moduleDescription,
            int modulePosition,
            UUID topicId,
            String topicTitle,
            String topicDescription,
            Integer topicPosition,
            TopicStatus topicStatus,
            StudentTopicProgressStatus progressStatus) {}

    private static final class MutableModule {
        private final UUID id;
        private final String title;
        private final String description;
        private final int position;
        private final List<ProgramTopic> topics = new ArrayList<>();

        private MutableModule(UUID id, String title, String description, int position) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.position = position;
        }

        private ProgramModule toProgramModule() {
            return new ProgramModule(id, title, description, position, List.copyOf(topics));
        }
    }
}
