package com.tutorplatform.program.domain.learningprogram;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class LearningProgramEntity {

    private final UUID id;
    private final UUID teacherId;
    private final UUID subjectId;
    private final Long version;
    private final Instant createdAt;
    private final Instant updatedAt;
    private String title;
    private String description;
    private LearningProgramStatus status;

    public LearningProgramEntity(
        UUID id,
        UUID teacherId,
        UUID subjectId,
        String title,
        String description,
        LearningProgramStatus status
    ) {
        this(id, teacherId, subjectId, title, description, status, null, null, null);
    }

    public LearningProgramEntity(
        UUID id,
        UUID teacherId,
        UUID subjectId,
        String title,
        String description,
        LearningProgramStatus status,
        Long version,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.teacherId = Objects.requireNonNull(teacherId);
        this.subjectId = Objects.requireNonNull(subjectId);
        this.title = Objects.requireNonNull(title);
        this.description = description;
        this.status = Objects.requireNonNull(status);
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void update(String title, String description) {
        this.title = Objects.requireNonNull(title);
        this.description = description;
    }

    public void activate() {
        if (status != LearningProgramStatus.DRAFT) {
            throw new IllegalStateException("Only a draft learning program can be activated");
        }
        status = LearningProgramStatus.ACTIVE;
    }

    public void archive() {
        status = LearningProgramStatus.ARCHIVED;
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

    public String getDescription() {
        return description;
    }

    public LearningProgramStatus getStatus() {
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
