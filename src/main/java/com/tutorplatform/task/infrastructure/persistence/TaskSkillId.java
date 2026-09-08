package com.tutorplatform.task.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class TaskSkillId implements Serializable {

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "skill_id", nullable = false)
    private UUID skillId;

    protected TaskSkillId() {
    }

    public TaskSkillId(UUID taskId, UUID skillId) {
        this.taskId = Objects.requireNonNull(taskId);
        this.skillId = Objects.requireNonNull(skillId);
    }

    public UUID getTaskId() { return taskId; }
    public UUID getSkillId() { return skillId; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TaskSkillId that)) return false;
        return taskId.equals(that.taskId) && skillId.equals(that.skillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, skillId);
    }
}
