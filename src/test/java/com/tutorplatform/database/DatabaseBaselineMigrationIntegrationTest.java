package com.tutorplatform.database;

import static org.assertj.core.api.Assertions.assertThat;

import com.tutorplatform.test.PostgresIntegrationTest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DatabaseBaselineMigrationIntegrationTest extends PostgresIntegrationTest {

    private static final String DATABASE = "test_database_baseline_migration";
    private static final String PYTHON_SUBJECT_ID = "6513554d-dceb-5902-b6df-557c7a94b5c7";

    private static Flyway flyway;

    @BeforeAll
    static void migrateCleanDatabase() {
        PostgresIntegrationTest.ensureDatabase(DATABASE);
        flyway =
                Flyway.configure()
                        .dataSource(
                                PostgresIntegrationTest.jdbcUrlForDatabase(DATABASE),
                                POSTGRES.getUsername(),
                                POSTGRES.getPassword())
                        .locations("classpath:db/migration")
                        .load();
        flyway.migrate();
    }

    @Test
    void cleanDatabaseMigratesThroughV013() throws SQLException {
        assertThat(
                        Arrays.stream(flyway.info().applied())
                                .map(migration -> migration.getVersion().toString()))
                .containsExactly(
                        "001", "002", "003", "004", "005", "006", "007", "008", "009", "010", "011",
                        "012", "013");

        assertThat(
                        queryStrings(
                                """
            select table_name
            from information_schema.tables
            where table_schema = 'public'
              and table_name <> 'flyway_schema_history'
            """))
                .containsExactlyInAnyOrder(
                        "users",
                        "user_roles",
                        "teachers",
                        "students",
                        "teacher_student_links",
                        "student_invites",
                        "subjects",
                        "learning_programs",
                        "student_programs",
                        "modules",
                        "topics",
                        "student_topic_progress",
                        "file_assets",
                        "lesson_materials",
                        "lesson_sessions",
                        "lesson_session_topics",
                        "teacher_assessments",
                        "tasks",
                        "topic_tasks",
                        "skills",
                        "task_skills",
                        "programming_task_configs",
                        "task_test_cases",
                        "homeworks",
                        "homework_items",
                        "submissions",
                        "code_submissions",
                        "learning_periods",
                        "progress_reports",
                        "progress_shares",
                        "report_shares",
                        "platform_settings",
                        "teacher_registration_invites");
    }

    @Test
    void pythonSystemSubjectIsSeededExactlyOnceAndProtectedByUniqueIndex() throws SQLException {
        try (Connection connection = connection();
                var statement =
                        connection.prepareStatement(
                                """
                 select id::text, owner_teacher_id, code, name, description, status
                 from subjects
                 where owner_teacher_id is null and code = 'PYTHON'
                 """);
                var rows = statement.executeQuery()) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getString("id")).isEqualTo(PYTHON_SUBJECT_ID);
            assertThat(rows.getObject("owner_teacher_id")).isNull();
            assertThat(rows.getString("code")).isEqualTo("PYTHON");
            assertThat(rows.getString("name")).isEqualTo("Python");
            assertThat(rows.getString("description")).isNull();
            assertThat(rows.getString("status")).isEqualTo("ACTIVE");
            assertThat(rows.next()).isFalse();
        }

        IndexMetadata uniqueIndex = readIndexMetadata().get("uq_system_subject_code");
        assertThat(uniqueIndex).isNotNull();
        assertThat(uniqueIndex.tableName()).isEqualTo("subjects");
        assertThat(uniqueIndex.unique()).isTrue();
        assertThat(uniqueIndex.definition()).contains("(code)");
        assertThat(uniqueIndex.predicate())
                .contains("owner_teacher_id IS NULL")
                .contains("code IS NOT NULL");
    }

    @Test
    void performanceIndexesMatchErModelAndRemainNonUnique() throws SQLException {
        Map<String, IndexMetadata> actual = readIndexMetadata();
        Map<String, ExpectedIndex> expected =
                Map.ofEntries(
                        expected(
                                "ix_student_program_student_status",
                                "student_programs",
                                "(student_id, status)"),
                        expected(
                                "ix_topic_progress_program_status",
                                "student_topic_progress",
                                "(student_program_id, status)"),
                        expected(
                                "ix_session_program_started",
                                "lesson_sessions",
                                "(student_program_id, started_at DESC)"),
                        expected(
                                "ix_session_teacher_started",
                                "lesson_sessions",
                                "(teacher_id, started_at DESC)"),
                        expected(
                                "ix_task_teacher_subject_status",
                                "tasks",
                                "(teacher_id, subject_id, status)"),
                        expected(
                                "ix_homework_program_assigned",
                                "homeworks",
                                "(student_program_id, assigned_at DESC)"),
                        expected(
                                "ix_homework_program_due_active",
                                "homeworks",
                                "(student_program_id, due_at)",
                                "status",
                                "'ASSIGNED'"),
                        expected(
                                "ix_submission_program_submitted",
                                "submissions",
                                "(student_program_id, submitted_at DESC)"),
                        expected(
                                "ix_submission_student_task_submitted",
                                "submissions",
                                "(student_id, task_id, submitted_at DESC)"),
                        expected(
                                "ix_submission_homework_item_submitted",
                                "submissions",
                                "(homework_item_id, submitted_at DESC)",
                                "homework_item_id IS NOT NULL"),
                        expected(
                                "ix_report_program_created",
                                "progress_reports",
                                "(student_program_id, created_at DESC)"));

        expected.forEach(
                (name, expectedIndex) -> {
                    IndexMetadata index = actual.get(name);
                    assertThat(index).as(name).isNotNull();
                    assertThat(index.tableName()).as(name).isEqualTo(expectedIndex.tableName());
                    assertThat(index.unique()).as(name).isFalse();
                    assertThat(index.definition()).as(name).contains(expectedIndex.columns());
                    if (expectedIndex.predicateFragments().isEmpty()) {
                        assertThat(index.predicate()).as(name).isNull();
                    } else {
                        assertThat(index.predicate())
                                .as(name)
                                .contains(expectedIndex.predicateFragments());
                    }
                });
    }

    @Test
    void importantReadQueryShapesAreExplainable() throws SQLException {
        String id = "00000000-0000-0000-0000-000000000000";
        List<String> queries =
                List.of(
                        "select * from lesson_sessions where student_program_id = '%s' order by started_at desc"
                                .formatted(id),
                        "select * from homeworks where student_program_id = '%s' order by assigned_at desc"
                                .formatted(id),
                        "select * from homeworks where student_program_id = '%s' and status = 'ASSIGNED' order by due_at"
                                .formatted(id),
                        "select * from submissions where student_program_id = '%s' order by submitted_at desc"
                                .formatted(id),
                        "select * from submissions where student_id = '%1$s' and task_id = '%1$s' order by submitted_at desc"
                                .formatted(id),
                        "select * from progress_reports where student_program_id = '%s' order by created_at desc"
                                .formatted(id));

        for (String query : queries) {
            assertThat(queryStrings("explain " + query)).isNotEmpty();
        }
    }

    private static Map.Entry<String, ExpectedIndex> expected(
            String name, String tableName, String columns, String... predicateFragments) {
        return Map.entry(name, new ExpectedIndex(tableName, columns, List.of(predicateFragments)));
    }

    private static List<String> queryStrings(String sql) throws SQLException {
        try (Connection connection = connection();
                var statement = connection.prepareStatement(sql);
                var rows = statement.executeQuery()) {
            var values = new java.util.ArrayList<String>();
            while (rows.next()) {
                values.add(rows.getString(1));
            }
            return values;
        }
    }

    private static Map<String, IndexMetadata> readIndexMetadata() throws SQLException {
        Map<String, IndexMetadata> indexes = new HashMap<>();
        try (Connection connection = connection();
                var statement =
                        connection.prepareStatement(
                                """
                 select index_class.relname as index_name,
                        table_class.relname as table_name,
                        index.indisunique,
                        pg_get_indexdef(index.indexrelid) as definition,
                        pg_get_expr(index.indpred, index.indrelid) as predicate
                 from pg_index index
                 join pg_class index_class on index_class.oid = index.indexrelid
                 join pg_class table_class on table_class.oid = index.indrelid
                 join pg_namespace namespace on namespace.oid = table_class.relnamespace
                 where namespace.nspname = 'public'
                 """);
                var rows = statement.executeQuery()) {
            while (rows.next()) {
                indexes.put(
                        rows.getString("index_name"),
                        new IndexMetadata(
                                rows.getString("table_name"),
                                rows.getBoolean("indisunique"),
                                rows.getString("definition"),
                                rows.getString("predicate")));
            }
        }
        return indexes;
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(
                PostgresIntegrationTest.jdbcUrlForDatabase(DATABASE),
                POSTGRES.getUsername(),
                POSTGRES.getPassword());
    }

    private record ExpectedIndex(
            String tableName, String columns, List<String> predicateFragments) {}

    private record IndexMetadata(
            String tableName, boolean unique, String definition, String predicate) {}
}
