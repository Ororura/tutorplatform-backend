package com.tutorplatform.report.infrastructure.persistence;

import com.tutorplatform.report.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class ProgressReportPersistenceIntegrationTest {

    private static final Instant START = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant END = Instant.parse("2026-02-01T10:00:00Z");

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private ProgressReportRepository repository;
    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.target", () -> "008");
    }

    @Test
    void snapshotJsonbRoundTripsWithSchemaVersionOneAndNullablePeriod() {
        Fixture fixture = fixture();
        ProgressReport saved = repository.saveAndFlush(report(fixture, null, ProgressReportStatus.DRAFT));

        ProgressReport loaded = repository.findById(saved.id()).orElseThrow();

        assertThat(loaded.snapshotSchemaVersion()).isEqualTo(ProgressReportSnapshotSchemas.V1);
        assertThat(loaded.snapshot()).isEqualTo(saved.snapshot());
        assertThat(loaded.learningPeriodId()).isNull();
        assertThat(jdbc.queryForObject(
            "SELECT jsonb_typeof(snapshot_json) FROM progress_reports WHERE id = ?",
            String.class, saved.id()
        )).isEqualTo("object");
    }

    @ParameterizedTest
    @EnumSource(ProgressReportStatus.class)
    void persistsEverySchemaStatus(ProgressReportStatus status) {
        Fixture fixture = fixture();
        ProgressReport saved = repository.saveAndFlush(report(fixture, null, status));

        assertThat(repository.findById(saved.id()).orElseThrow().status()).isEqualTo(status);
    }

    @Test
    void duplicateNonNullLearningPeriodIsRejected() {
        Fixture fixture = fixture();
        UUID periodId = completedPeriod(fixture);
        repository.saveAndFlush(report(fixture, periodId, ProgressReportStatus.DRAFT));

        assertThatThrownBy(() -> repository.saveAndFlush(
            report(fixture, periodId, ProgressReportStatus.DRAFT)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void negativeLearningMinutesIsRejectedByDatabase() {
        Fixture fixture = fixture();
        assertThatThrownBy(() -> jdbc.update("""
            INSERT INTO progress_reports(
                id, student_program_id, generated_by_teacher_id, status,
                period_started_at, period_ended_at, learning_minutes,
                snapshot_schema_version, snapshot_json
            ) VALUES (?, ?, ?, 'DRAFT', ?, ?, -1, 1, '{}'::jsonb)
            """, UUID.randomUUID(), fixture.studentProgramId(), fixture.teacherId(),
            Timestamp.from(START), Timestamp.from(END)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void versionIncrementsAndStaleUpdateIsRejected() {
        Fixture fixture = fixture();
        ProgressReport original = repository.saveAndFlush(
            report(fixture, null, ProgressReportStatus.DRAFT)
        );
        ProgressReport changed = repository.saveAndFlush(
            original.editDraft("Summary", "Plan", original.version(), END.plusSeconds(1))
        );

        assertThat(changed.version()).isEqualTo(original.version() + 1);
        assertThatThrownBy(() -> repository.saveAndFlush(
            original.editDraft("Stale", null, original.version(), END.plusSeconds(2))
        )).isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    private ProgressReport report(
        Fixture fixture,
        UUID periodId,
        ProgressReportStatus status
    ) {
        Instant publishedAt = status == ProgressReportStatus.PUBLISHED ? END.plusSeconds(1) : null;
        return new ProgressReport(
            UUID.randomUUID(), fixture.studentProgramId(), periodId, fixture.teacherId(), status,
            START, END, 510, ProgressReportSnapshotSchemas.V1, snapshot(), null, null,
            publishedAt, 0, START, START
        );
    }

    private ProgressReportSnapshotV1 snapshot() {
        return new ProgressReportSnapshotV1(
            new ProgressReportSnapshotV1.Metrics(510, 6, 1.0, 6, 5, 38, 31),
            new ProgressReportSnapshotV1.Assessment(
                new BigDecimal("4.4"), new BigDecimal("4.0"),
                new BigDecimal("4.5"), new BigDecimal("4.2")
            ),
            new ProgressReportSnapshotV1.Topics(
                List.of(new ProgressReportSnapshotV1.Topic(UUID.randomUUID(), "Условия")),
                List.of(new ProgressReportSnapshotV1.Topic(UUID.randomUUID(), "Циклы"))
            ),
            List.of()
        );
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
        return new Fixture(teacherId, studentProgramId);
    }

    private UUID completedPeriod(Fixture fixture) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO learning_periods(
                id, student_program_id, sequence_no, start_cumulative_minutes,
                target_duration_minutes, end_cumulative_minutes, status, started_at, completed_at
            ) VALUES (?, ?, 1, 0, 480, 510, 'COMPLETED', ?, ?)
            """, id, fixture.studentProgramId(), Timestamp.from(START), Timestamp.from(END));
        return id;
    }

    private record Fixture(UUID teacherId, UUID studentProgramId) {
    }
}
