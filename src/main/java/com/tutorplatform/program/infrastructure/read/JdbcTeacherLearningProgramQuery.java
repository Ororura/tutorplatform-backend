package com.tutorplatform.program.infrastructure.read;

import com.tutorplatform.program.application.TeacherLearningProgramQuery;
import com.tutorplatform.program.domain.TopicStatus;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class JdbcTeacherLearningProgramQuery implements TeacherLearningProgramQuery {
    private final JdbcClient jdbcClient;

    public JdbcTeacherLearningProgramQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<LearningProgramSummary> findPrograms(UUID teacherId, LearningProgramStatus status) {
        String statusClause = status == null ? "" : " AND program.status = :status";
        JdbcClient.StatementSpec statement = jdbcClient.sql("""
            SELECT program.id, program.slug, program.title, program.description, program.status,
                   program.created_at, program.updated_at,
                   subject.id AS subject_id, subject.code AS subject_code, subject.name AS subject_name
            FROM learning_programs program
            JOIN subjects subject ON subject.id = program.subject_id
            WHERE program.teacher_id = :teacherId
            """ + statusClause + " ORDER BY program.created_at DESC, program.id ASC")
            .param("teacherId", teacherId);
        if (status != null) {
            statement = statement.param("status", status.name());
        }
        return statement.query((resultSet, rowNumber) -> new LearningProgramSummary(
            resultSet.getObject("id", UUID.class),
            resultSet.getString("slug"),
            resultSet.getObject("subject_id", UUID.class),
            resultSet.getString("subject_code"),
            resultSet.getString("subject_name"),
            resultSet.getString("title"),
            resultSet.getString("description"),
            LearningProgramStatus.valueOf(resultSet.getString("status")),
            resultSet.getTimestamp("created_at").toInstant(),
            resultSet.getTimestamp("updated_at").toInstant()
        )).list();
    }

    @Override
    public Optional<LearningProgramDetails> findProgram(UUID teacherId, UUID learningProgramId) {
        return jdbcClient.sql("""
            SELECT program.id, program.slug, program.title, program.description, program.status, program.version,
                   program.created_at, program.updated_at,
                   subject.id AS subject_id, subject.code AS subject_code, subject.name AS subject_name
            FROM learning_programs program
            JOIN subjects subject ON subject.id = program.subject_id
            WHERE program.id = :learningProgramId AND program.teacher_id = :teacherId
            """)
            .param("learningProgramId", learningProgramId)
            .param("teacherId", teacherId)
            .query((resultSet, rowNumber) -> new ProgramRow(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("slug"),
                resultSet.getObject("subject_id", UUID.class),
                resultSet.getString("subject_code"),
                resultSet.getString("subject_name"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                LearningProgramStatus.valueOf(resultSet.getString("status")),
                resultSet.getObject("version", Long.class),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant()
            )).optional()
            .map(program -> new LearningProgramDetails(
                program.id(), program.slug(), program.subjectId(), program.subjectCode(), program.subjectName(), program.title(),
                program.description(), program.status(), program.version(), program.createdAt(), program.updatedAt(),
                hasAssignments(program.id()), findModules(program.id())
            ));
    }

    @Override
    public Optional<UUID> findProgramIdBySlug(UUID teacherId, String slug) {
        return jdbcClient.sql("""
            SELECT id
            FROM learning_programs
            WHERE teacher_id = :teacherId
              AND slug = :slug
            """)
            .param("teacherId", teacherId)
            .param("slug", slug)
            .query(UUID.class)
            .optional();
    }

    @Override
    public Optional<String> findSlug(UUID teacherId, UUID learningProgramId) {
        return jdbcClient.sql("""
            SELECT slug
            FROM learning_programs
            WHERE teacher_id = :teacherId
              AND id = :learningProgramId
            """)
            .param("teacherId", teacherId)
            .param("learningProgramId", learningProgramId)
            .query(String.class)
            .optional();
    }

    @Override
    public Optional<String> findTopicSlug(UUID learningProgramId, UUID topicId) {
        return jdbcClient.sql("""
            SELECT topic.slug
            FROM topics topic
            JOIN modules module ON module.id = topic.module_id
            WHERE module.learning_program_id = :learningProgramId
              AND topic.id = :topicId
            """)
            .param("learningProgramId", learningProgramId)
            .param("topicId", topicId)
            .query(String.class)
            .optional();
    }

    @Override
    public List<UUID> findTopicIds(UUID learningProgramId) {
        return jdbcClient.sql("""
            SELECT topic.id
            FROM modules module
            JOIN topics topic ON topic.module_id = module.id
            WHERE module.learning_program_id = :learningProgramId
            ORDER BY module.position ASC, module.id ASC, topic.position ASC, topic.id ASC
            """)
            .param("learningProgramId", learningProgramId)
            .query(UUID.class)
            .list();
    }

    private boolean hasAssignments(UUID learningProgramId) {
        return jdbcClient.sql("""
            SELECT EXISTS (
                SELECT 1 FROM student_programs WHERE learning_program_id = :learningProgramId
            )
            """)
            .param("learningProgramId", learningProgramId)
            .query(Boolean.class)
            .single();
    }

    private List<ModuleDetails> findModules(UUID learningProgramId) {
        List<ModuleRow> modules = jdbcClient.sql("""
            SELECT id, title, description, position
            FROM modules
            WHERE learning_program_id = :learningProgramId
            ORDER BY position ASC, id ASC
            """)
            .param("learningProgramId", learningProgramId)
            .query((resultSet, rowNumber) -> new ModuleRow(
                resultSet.getObject("id", UUID.class), resultSet.getString("title"),
                resultSet.getString("description"), resultSet.getInt("position")
            )).list();
        if (modules.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<TopicDetails>> topicsByModuleId = jdbcClient.sql("""
            SELECT id, module_id, slug, title, description, position, status, version
            FROM topics
            WHERE module_id IN (:moduleIds)
            ORDER BY module_id ASC, position ASC, id ASC
            """)
            .param("moduleIds", modules.stream().map(ModuleRow::id).toList())
            .query((resultSet, rowNumber) -> new TopicRow(
                resultSet.getObject("id", UUID.class), resultSet.getObject("module_id", UUID.class),
                resultSet.getString("slug"),
                resultSet.getString("title"), resultSet.getString("description"), resultSet.getInt("position"),
                TopicStatus.valueOf(resultSet.getString("status")), resultSet.getObject("version", Long.class)
            )).list().stream().collect(Collectors.groupingBy(TopicRow::moduleId,
                Collectors.mapping(topic -> new TopicDetails(
                    topic.id(), topic.slug(), topic.title(), topic.description(), topic.position(), topic.status(), topic.version()
                ), Collectors.toList())));
        return modules.stream().map(module -> new ModuleDetails(
            module.id(), module.title(), module.description(), module.position(),
            topicsByModuleId.getOrDefault(module.id(), List.of())
        )).toList();
    }

    private record ProgramRow(
        UUID id, String slug, UUID subjectId, String subjectCode, String subjectName, String title, String description,
        LearningProgramStatus status, Long version, java.time.Instant createdAt, java.time.Instant updatedAt
    ) {
    }

    private record ModuleRow(UUID id, String title, String description, int position) {
    }

    private record TopicRow(
        UUID id, UUID moduleId, String slug, String title, String description, int position, TopicStatus status, Long version
    ) {
    }
}
