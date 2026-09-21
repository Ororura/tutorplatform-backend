package com.tutorplatform.program.domain.studentprogram;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record StudentTopicProgressEntity(
        UUID studentProgramId,
        UUID topicId,
        StudentTopicProgressStatus status,
        Instant startedAt,
        Instant completedAt,
        Instant updatedAt) {

    public StudentTopicProgressEntity(
            UUID studentProgramId,
            UUID topicId,
            StudentTopicProgressStatus status,
            Instant startedAt,
            Instant completedAt) {
        this(studentProgramId, topicId, status, startedAt, completedAt, null);
    }

    public StudentTopicProgressEntity(
            UUID studentProgramId,
            UUID topicId,
            StudentTopicProgressStatus status,
            Instant startedAt,
            Instant completedAt,
            Instant updatedAt) {
        this.studentProgramId = Objects.requireNonNull(studentProgramId);
        this.topicId = Objects.requireNonNull(topicId);
        this.status = Objects.requireNonNull(status);
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.updatedAt = updatedAt;
    }
}
