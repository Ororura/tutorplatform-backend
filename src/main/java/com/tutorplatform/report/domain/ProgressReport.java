package com.tutorplatform.report.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ProgressReport(
    UUID id,
    UUID studentProgramId,
    UUID learningPeriodId,
    UUID generatedByTeacherId,
    ProgressReportStatus status,
    Instant periodStartedAt,
    Instant periodEndedAt,
    int learningMinutes,
    int snapshotSchemaVersion,
    ProgressReportSnapshotV1 snapshot,
    String teacherSummary,
    String nextPeriodPlan,
    Instant publishedAt,
    long version,
    Instant createdAt,
    Instant updatedAt
) {
    public ProgressReport {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(studentProgramId, "studentProgramId");
        Objects.requireNonNull(generatedByTeacherId, "generatedByTeacherId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(periodStartedAt, "periodStartedAt");
        Objects.requireNonNull(periodEndedAt, "periodEndedAt");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (periodEndedAt.isBefore(periodStartedAt)) {
            throw new IllegalArgumentException("periodEndedAt must not precede periodStartedAt");
        }
        if (learningMinutes < 0) {
            throw new IllegalArgumentException("learningMinutes must not be negative");
        }
        if (snapshotSchemaVersion != ProgressReportSnapshotSchemas.V1) {
            throw new IllegalArgumentException("unsupported snapshot schema version");
        }
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
        if (status == ProgressReportStatus.DRAFT && publishedAt != null) {
            throw new IllegalArgumentException("DRAFT report cannot have publishedAt");
        }
        if (status == ProgressReportStatus.PUBLISHED && publishedAt == null) {
            throw new IllegalArgumentException("PUBLISHED report requires publishedAt");
        }
    }

    public static ProgressReport draft(
        UUID id,
        UUID studentProgramId,
        UUID learningPeriodId,
        UUID generatedByTeacherId,
        Instant periodStartedAt,
        Instant periodEndedAt,
        int learningMinutes,
        ProgressReportSnapshotV1 snapshot,
        Instant now
    ) {
        return new ProgressReport(
            id, studentProgramId, learningPeriodId, generatedByTeacherId,
            ProgressReportStatus.DRAFT, periodStartedAt, periodEndedAt, learningMinutes,
            ProgressReportSnapshotSchemas.V1, snapshot, null, null, null, 0, now, now
        );
    }

    public ProgressReport editDraft(
        String teacherSummary,
        String nextPeriodPlan,
        long expectedVersion,
        Instant now
    ) {
        requireDraft();
        return new ProgressReport(
            id, studentProgramId, learningPeriodId, generatedByTeacherId, status,
            periodStartedAt, periodEndedAt, learningMinutes, snapshotSchemaVersion, snapshot,
            teacherSummary, nextPeriodPlan, null, expectedVersion, createdAt, now
        );
    }

    public ProgressReport publish(long expectedVersion, Instant now) {
        requireDraft();
        return new ProgressReport(
            id, studentProgramId, learningPeriodId, generatedByTeacherId,
            ProgressReportStatus.PUBLISHED, periodStartedAt, periodEndedAt, learningMinutes,
            snapshotSchemaVersion, snapshot, teacherSummary, nextPeriodPlan,
            Objects.requireNonNull(now, "now"), expectedVersion, createdAt, now
        );
    }

    private void requireDraft() {
        if (status != ProgressReportStatus.DRAFT) {
            throw new IllegalStateException("only a DRAFT progress report can be changed");
        }
    }
}
