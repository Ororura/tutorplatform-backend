package com.tutorplatform.program.domain.studentprogram;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class StudentProgramEntity {

    private final UUID id;
    private final UUID studentId;
    private final UUID learningProgramId;
    private final UUID assignedByTeacherId;
    private final StudentProgramStatus status;
    private final int reportIntervalMinutes;
    private final Instant startedAt;
    private final Instant completedAt;
    private final Long version;
    private final Instant createdAt;
    private final Instant updatedAt;

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

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public UUID getLearningProgramId() { return learningProgramId; }
    public UUID getAssignedByTeacherId() { return assignedByTeacherId; }
    public StudentProgramStatus getStatus() { return status; }
    public int getReportIntervalMinutes() { return reportIntervalMinutes; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
