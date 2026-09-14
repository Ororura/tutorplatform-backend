package com.tutorplatform.program.infrastructure.read;

import com.tutorplatform.program.application.TeacherLearningProgramQuery;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

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
            SELECT program.id, program.title, program.description, program.status,
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
}
