package com.tutorplatform.task.domain.taskskill;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TaskSkillEntity(UUID taskId, UUID skillId, Instant createdAt) {

    public TaskSkillEntity(UUID taskId, UUID skillId) {
        this(taskId, skillId, null);
    }

    public TaskSkillEntity(UUID taskId, UUID skillId, Instant createdAt) {
        this.taskId = Objects.requireNonNull(taskId);
        this.skillId = Objects.requireNonNull(skillId);
        this.createdAt = createdAt;
    }
}
