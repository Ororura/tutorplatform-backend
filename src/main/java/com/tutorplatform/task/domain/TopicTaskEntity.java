package com.tutorplatform.task.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class TopicTaskEntity {

    private final UUID topicId;
    private final UUID taskId;
    private final int position;
    private final boolean required;
    private final Instant createdAt;

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

    public UUID getTopicId() { return topicId; }
    public UUID getTaskId() { return taskId; }
    public int getPosition() { return position; }
    public boolean isRequired() { return required; }
    public Instant getCreatedAt() { return createdAt; }
}
