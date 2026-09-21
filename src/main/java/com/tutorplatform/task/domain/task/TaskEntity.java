package com.tutorplatform.task.domain.task;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class TaskEntity {

    private final UUID id;
    private final UUID teacherId;
    private final UUID subjectId;
    private final Long version;
    private final Instant createdAt;
    private final Instant updatedAt;
    private String title;
    private String descriptionMarkdown;
    private TaskType taskType;
    private TaskDifficulty difficulty;
    private TaskStatus status;

    public TaskEntity(
            UUID id,
            UUID teacherId,
            UUID subjectId,
            String title,
            String descriptionMarkdown,
            TaskType taskType,
            TaskDifficulty difficulty,
            TaskStatus status) {
        this(
                id,
                teacherId,
                subjectId,
                title,
                descriptionMarkdown,
                taskType,
                difficulty,
                status,
                null,
                null,
                null);
    }

    public TaskEntity(
            UUID id,
            UUID teacherId,
            UUID subjectId,
            String title,
            String descriptionMarkdown,
            TaskType taskType,
            TaskDifficulty difficulty,
            TaskStatus status,
            Long version,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.teacherId = Objects.requireNonNull(teacherId);
        this.subjectId = Objects.requireNonNull(subjectId);
        applyChanges(title, descriptionMarkdown, taskType, difficulty, status);
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void update(
            String title,
            String descriptionMarkdown,
            TaskType taskType,
            TaskDifficulty difficulty,
            TaskStatus status) {
        applyChanges(title, descriptionMarkdown, taskType, difficulty, status);
    }

    private void applyChanges(
            String title,
            String descriptionMarkdown,
            TaskType taskType,
            TaskDifficulty difficulty,
            TaskStatus status) {
        this.title = Objects.requireNonNull(title);
        this.descriptionMarkdown = Objects.requireNonNull(descriptionMarkdown);
        this.taskType = Objects.requireNonNull(taskType);
        this.difficulty = Objects.requireNonNull(difficulty);
        this.status = Objects.requireNonNull(status);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTeacherId() {
        return teacherId;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescriptionMarkdown() {
        return descriptionMarkdown;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public TaskDifficulty getDifficulty() {
        return difficulty;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
