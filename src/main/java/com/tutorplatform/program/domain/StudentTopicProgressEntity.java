package com.tutorplatform.program.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class StudentTopicProgressEntity {

    private final UUID studentProgramId;
    private final UUID topicId;
    private final StudentTopicProgressStatus status;
    private final Instant startedAt;
    private final Instant completedAt;
    private final Instant updatedAt;

    public StudentTopicProgressEntity(
            UUID studentProgramId,
            UUID topicId,
            StudentTopicProgressStatus status,
            Instant startedAt,
            Instant completedAt
    ) {
        this(studentProgramId, topicId, status, startedAt, completedAt, null);
    }

    public StudentTopicProgressEntity(
            UUID studentProgramId,
            UUID topicId,
            StudentTopicProgressStatus status,
            Instant startedAt,
            Instant completedAt,
            Instant updatedAt
    ) {
        this.studentProgramId = Objects.requireNonNull(studentProgramId);
        this.topicId = Objects.requireNonNull(topicId);
        this.status = Objects.requireNonNull(status);
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.updatedAt = updatedAt;
    }

    public UUID getStudentProgramId() { return studentProgramId; }
    public UUID getTopicId() { return topicId; }
    public StudentTopicProgressStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
