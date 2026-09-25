package com.tutorplatform.content.infrastructure.persistence;

import com.tutorplatform.content.application.importpackage.ContentPackageImportRecord;
import com.tutorplatform.content.application.importpackage.ContentPackageImportRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcContentPackageImportRepository implements ContentPackageImportRepository {

    private final JdbcTemplate jdbc;

    public JdbcContentPackageImportRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<ContentPackageImportRecord> findByConfirmation(
            UUID teacherId, UUID learningProgramId, UUID confirmationId) {
        return jdbc
                .query(
                        """
                        SELECT id, teacher_id, learning_program_id, confirmation_id,
                               package_digest, module_count, topic_count, material_count,
                               created_module_ids, created_at
                        FROM content_package_imports
                        WHERE teacher_id = ? AND learning_program_id = ? AND confirmation_id = ?
                        """,
                        JdbcContentPackageImportRepository::mapRecord,
                        teacherId,
                        learningProgramId,
                        confirmationId)
                .stream()
                .findFirst();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<ContentPackageImportRecord> registerConfirmation(
            UUID teacherId, UUID learningProgramId, UUID confirmationId) {
        // A transaction-scoped PostgreSQL lock prevents concurrent callers from creating content
        // for the same confirmation. A hash collision only causes extra serialization.
        String key = teacherId + ":" + learningProgramId + ":" + confirmationId;
        jdbc.queryForObject(
                "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", Object.class, key);
        return findByConfirmation(teacherId, learningProgramId, confirmationId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public ContentPackageImportRecord saveSuccessfulImport(
            UUID teacherId,
            UUID learningProgramId,
            UUID confirmationId,
            String packageDigest,
            int moduleCount,
            int topicCount,
            int materialCount,
            List<UUID> createdModuleIds) {
        UUID id = UUID.randomUUID();
        return jdbc.query(
                        connection -> {
                            var statement =
                                    connection.prepareStatement(
                                            """
                                            INSERT INTO content_package_imports
                                                (id, teacher_id, learning_program_id,
                                                 confirmation_id, package_digest, module_count,
                                                 topic_count, material_count, created_module_ids)
                                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                                            RETURNING id, teacher_id, learning_program_id,
                                                      confirmation_id, package_digest, module_count,
                                                      topic_count, material_count, created_module_ids,
                                                      created_at
                                            """);
                            statement.setObject(1, id);
                            statement.setObject(2, teacherId);
                            statement.setObject(3, learningProgramId);
                            statement.setObject(4, confirmationId);
                            statement.setString(5, packageDigest);
                            statement.setInt(6, moduleCount);
                            statement.setInt(7, topicCount);
                            statement.setInt(8, materialCount);
                            statement.setArray(
                                    9,
                                    connection.createArrayOf(
                                            "uuid", createdModuleIds.toArray(UUID[]::new)));
                            return statement;
                        },
                        JdbcContentPackageImportRepository::mapRecord)
                .getFirst();
    }

    private static ContentPackageImportRecord mapRecord(ResultSet row, int rowNumber)
            throws SQLException {
        return new ContentPackageImportRecord(
                row.getObject("id", UUID.class),
                row.getObject("teacher_id", UUID.class),
                row.getObject("learning_program_id", UUID.class),
                row.getObject("confirmation_id", UUID.class),
                row.getString("package_digest"),
                row.getInt("module_count"),
                row.getInt("topic_count"),
                row.getInt("material_count"),
                Arrays.asList((UUID[]) row.getArray("created_module_ids").getArray()),
                row.getTimestamp("created_at").toInstant());
    }
}
