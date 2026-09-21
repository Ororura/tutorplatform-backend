package com.tutorplatform.homework.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record HomeworkItemEntity(
        UUID id, UUID homeworkId, UUID taskId, int position, boolean required, Instant createdAt) {

    public HomeworkItemEntity(
            UUID id, UUID homeworkId, UUID taskId, int position, boolean required) {
        this(id, homeworkId, taskId, position, required, null);
    }

    public HomeworkItemEntity(
            UUID id,
            UUID homeworkId,
            UUID taskId,
            int position,
            boolean required,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.homeworkId = Objects.requireNonNull(homeworkId);
        this.taskId = Objects.requireNonNull(taskId);
        if (position < 0) {
            throw new IllegalArgumentException("position must be greater than or equal to 0");
        }
        this.position = position;
        this.required = required;
        this.createdAt = createdAt;
    }
}
