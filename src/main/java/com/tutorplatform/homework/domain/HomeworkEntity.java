package com.tutorplatform.homework.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class HomeworkEntity {

    private final UUID id;
    private final UUID studentProgramId;
    private final UUID assignedByTeacherId;
    private String title;
    private String description;
    private final Instant assignedAt;
    private Instant dueAt;
    private HomeworkStatus status;
    private Instant completedAt;
    private List<HomeworkItemEntity> items;
    private final Long version;
    private final Instant createdAt;
    private final Instant updatedAt;

    public HomeworkEntity(
            UUID id,
            UUID studentProgramId,
            UUID assignedByTeacherId,
            String title,
            String description,
            Instant assignedAt,
            Instant dueAt,
            HomeworkStatus status,
            Instant completedAt,
            List<HomeworkItemEntity> items
    ) {
        this(id, studentProgramId, assignedByTeacherId, title, description, assignedAt, dueAt,
                status, completedAt, items, null, null, null);
    }

    public HomeworkEntity(
            UUID id,
            UUID studentProgramId,
            UUID assignedByTeacherId,
            String title,
            String description,
            Instant assignedAt,
            Instant dueAt,
            HomeworkStatus status,
            Instant completedAt,
            List<HomeworkItemEntity> items,
            Long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.studentProgramId = Objects.requireNonNull(studentProgramId);
        this.assignedByTeacherId = Objects.requireNonNull(assignedByTeacherId);
        this.assignedAt = Objects.requireNonNull(assignedAt);
        applyChanges(title, description, dueAt, status, completedAt);
        replaceItems(items);
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void update(
            String title,
            String description,
            Instant dueAt,
            HomeworkStatus status,
            Instant completedAt
    ) {
        applyChanges(title, description, dueAt, status, completedAt);
    }

    public void replaceItems(List<HomeworkItemEntity> items) {
        List<HomeworkItemEntity> copy = List.copyOf(Objects.requireNonNull(items));
        if (copy.stream().anyMatch(item -> !id.equals(item.homeworkId()))) {
            throw new IllegalArgumentException("Every homework item must belong to this homework");
        }
        this.items = copy;
    }

    private void applyChanges(
            String title,
            String description,
            Instant dueAt,
            HomeworkStatus status,
            Instant completedAt
    ) {
        this.title = Objects.requireNonNull(title);
        this.description = description;
        this.dueAt = dueAt;
        this.status = Objects.requireNonNull(status);
        this.completedAt = completedAt;
    }

    public UUID getId() { return id; }
    public UUID getStudentProgramId() { return studentProgramId; }
    public UUID getAssignedByTeacherId() { return assignedByTeacherId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Instant getAssignedAt() { return assignedAt; }
    public Instant getDueAt() { return dueAt; }
    public HomeworkStatus getStatus() { return status; }
    public Instant getCompletedAt() { return completedAt; }
    public List<HomeworkItemEntity> getItems() { return items; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
