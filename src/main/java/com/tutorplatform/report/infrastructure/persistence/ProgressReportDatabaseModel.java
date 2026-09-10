package com.tutorplatform.report.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.tutorplatform.report.domain.ProgressReport;
import com.tutorplatform.report.domain.ProgressReportStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "progress_reports")
public class ProgressReportDatabaseModel {

    @Id
    private UUID id;

    @Column(name = "student_program_id", nullable = false, updatable = false)
    private UUID studentProgramId;

    @Column(name = "learning_period_id", updatable = false)
    private UUID learningPeriodId;

    @Column(name = "generated_by_teacher_id", nullable = false, updatable = false)
    private UUID generatedByTeacherId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ProgressReportStatus status;

    @Column(name = "period_started_at", nullable = false, updatable = false)
    private Instant periodStartedAt;

    @Column(name = "period_ended_at", nullable = false, updatable = false)
    private Instant periodEndedAt;

    @Column(name = "learning_minutes", nullable = false, updatable = false)
    private int learningMinutes;

    @Column(name = "snapshot_schema_version", nullable = false, updatable = false)
    private short snapshotSchemaVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot_json", nullable = false, updatable = false, columnDefinition = "jsonb")
    private JsonNode snapshotJson;

    @Column(name = "teacher_summary")
    private String teacherSummary;

    @Column(name = "next_period_plan")
    private String nextPeriodPlan;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProgressReportDatabaseModel() {
    }

    ProgressReportDatabaseModel(
        ProgressReport report,
        ProgressReportSnapshotJsonCodec codec,
        boolean existing
    ) {
        id = report.id();
        studentProgramId = report.studentProgramId();
        learningPeriodId = report.learningPeriodId();
        generatedByTeacherId = report.generatedByTeacherId();
        status = report.status();
        periodStartedAt = report.periodStartedAt();
        periodEndedAt = report.periodEndedAt();
        learningMinutes = report.learningMinutes();
        snapshotSchemaVersion = (short) report.snapshotSchemaVersion();
        snapshotJson = codec.writeV1(report.snapshot());
        teacherSummary = report.teacherSummary();
        nextPeriodPlan = report.nextPeriodPlan();
        publishedAt = report.publishedAt();
        version = existing ? report.version() : null;
        createdAt = report.createdAt();
        updatedAt = report.updatedAt();
    }

    ProgressReport toDomain(ProgressReportSnapshotJsonCodec codec) {
        return new ProgressReport(
            id, studentProgramId, learningPeriodId, generatedByTeacherId, status,
            periodStartedAt, periodEndedAt, learningMinutes, snapshotSchemaVersion,
            codec.read(snapshotSchemaVersion, snapshotJson), teacherSummary, nextPeriodPlan,
            publishedAt, version, createdAt, updatedAt
        );
    }
}
