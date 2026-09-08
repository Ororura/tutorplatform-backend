package com.tutorplatform.task.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class TaskSkillEntity {

    private final UUID taskId;
    private final UUID skillId;
    private final Instant createdAt;

    public TaskSkillEntity(UUID taskId, UUID skillId) {
        this(taskId, skillId, null);
    }

    public TaskSkillEntity(UUID taskId, UUID skillId, Instant createdAt) {
        this.taskId = Objects.requireNonNull(taskId);
        this.skillId = Objects.requireNonNull(skillId);
        this.createdAt = createdAt;
    }

    public UUID getTaskId() { return taskId; }
    public UUID getSkillId() { return skillId; }
    public Instant getCreatedAt() { return createdAt; }
}
