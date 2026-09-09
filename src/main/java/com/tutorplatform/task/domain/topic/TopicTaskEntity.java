package com.tutorplatform.task.domain.topic;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TopicTaskEntity(UUID topicId, UUID taskId, int position, boolean required, Instant createdAt) {

    public TopicTaskEntity(UUID topicId, UUID taskId, int position, boolean required) {
        this(topicId, taskId, position, required, null);
    }

    public TopicTaskEntity(
        UUID topicId,
        UUID taskId,
        int position,
        boolean required,
        Instant createdAt
    ) {
        this.topicId = Objects.requireNonNull(topicId);
        this.taskId = Objects.requireNonNull(taskId);
        if (position < 0) {
            throw new IllegalArgumentException("position must be greater than or equal to 0");
        }
        this.position = position;
        this.required = required;
        this.createdAt = createdAt;
    }
}
