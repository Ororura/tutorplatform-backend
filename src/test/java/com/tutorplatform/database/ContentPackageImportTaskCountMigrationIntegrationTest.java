package com.tutorplatform.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tutorplatform.test.PostgresIntegrationTest;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class ContentPackageImportTaskCountMigrationIntegrationTest extends PostgresIntegrationTest {

    @Test
    void existingImportGetsZeroTaskCountAndRejectsNegativeCounts() {
        String database = "test_import_task_count_" + UUID.randomUUID().toString().replace("-", "");
        ensureDatabase(database);
        String url = jdbcUrlForDatabase(database);
        var dataSource =
                new DriverManagerDataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword());
        var jdbc = new JdbcTemplate(dataSource);
        Flyway flyway =
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:db/migration")
                        .target("014")
                        .load();
        flyway.migrate();

        UUID userId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        UUID importId = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, email) VALUES (?, ?)", userId, userId + "@example.com");
        jdbc.update(
                "INSERT INTO teachers (id, user_id, display_name) VALUES (?, ?, 'Teacher')",
                teacherId,
                userId);
        jdbc.update(
                "INSERT INTO learning_programs (id, teacher_id, subject_id, title) VALUES (?, ?, (SELECT id FROM subjects WHERE code = 'PYTHON' AND owner_teacher_id IS NULL), 'Program')",
                programId,
                teacherId);
        jdbc.update(
                "INSERT INTO content_package_imports (id, teacher_id, learning_program_id, confirmation_id, package_digest, module_count, topic_count, material_count, created_module_ids) VALUES (?, ?, ?, ?, ?, 1, 2, 3, ARRAY[?]::uuid[])",
                importId,
                teacherId,
                programId,
                UUID.randomUUID(),
                "a".repeat(64),
                UUID.randomUUID());

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        assertThat(
                        jdbc.queryForObject(
                                "SELECT task_count FROM content_package_imports WHERE id = ?",
                                Integer.class,
                                importId))
                .isZero();
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE content_package_imports SET task_count = -1 WHERE id = ?",
                                        importId))
                .hasMessageContaining("ck_content_package_import_task_count");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT task_count FROM content_package_imports WHERE id = ?",
                                Integer.class,
                                importId))
                .isZero();
    }
}
