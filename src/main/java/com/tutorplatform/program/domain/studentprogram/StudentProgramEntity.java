package com.tutorplatform.program.domain.studentprogram;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record StudentProgramEntity(UUID id, UUID studentId, UUID learningProgramId, UUID assignedByTeacherId,
                                   StudentProgramStatus status, int reportIntervalMinutes, Instant startedAt,
                                   Instant completedAt, Long version, Instant createdAt, Instant updatedAt) {

    public static final int DEFAULT_REPORT_INTERVAL_MINUTES = 480;

    public StudentProgramEntity(
        UUID id,
        UUID studentId,
        UUID learningProgramId,
        UUID assignedByTeacherId,
        StudentProgramStatus status,
        int reportIntervalMinutes,
        Instant startedAt,
        Instant completedAt
    ) {
        this(id, studentId, learningProgramId, assignedByTeacherId, status, reportIntervalMinutes,
            startedAt, completedAt, null, null, null);
    }

    public StudentProgramEntity(
        UUID id,
        UUID studentId,
        UUID learningProgramId,
        UUID assignedByTeacherId,
        StudentProgramStatus status,
        int reportIntervalMinutes,
        Instant startedAt,
        Instant completedAt,
        Long version,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.studentId = Objects.requireNonNull(studentId);
        this.learningProgramId = Objects.requireNonNull(learningProgramId);
        this.assignedByTeacherId = Objects.requireNonNull(assignedByTeacherId);
        this.status = Objects.requireNonNull(status);
        if (reportIntervalMinutes <= 0) {
            throw new IllegalArgumentException("reportIntervalMinutes must be greater than 0");
        }
        this.reportIntervalMinutes = reportIntervalMinutes;
        this.startedAt = Objects.requireNonNull(startedAt);
        this.completedAt = completedAt;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
