package com.tutorplatform.task.infrastructure.persistence.topic;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class TopicTaskId implements Serializable {

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    protected TopicTaskId() {}

    public TopicTaskId(UUID topicId, UUID taskId) {
        this.topicId = Objects.requireNonNull(topicId);
        this.taskId = Objects.requireNonNull(taskId);
    }

    public UUID getTopicId() {
        return topicId;
    }

    public UUID getTaskId() {
        return taskId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TopicTaskId that)) return false;
        return topicId.equals(that.topicId) && taskId.equals(that.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicId, taskId);
    }
}
