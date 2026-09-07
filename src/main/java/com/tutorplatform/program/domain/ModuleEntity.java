package com.tutorplatform.program.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ModuleEntity {

    private final UUID id;
    private final UUID learningProgramId;
    private final String title;
    private final String description;
    private final int position;
    private final Instant createdAt;
    private final Instant updatedAt;

    public ModuleEntity(UUID id, UUID learningProgramId, String title, String description, int position) {
        this(id, learningProgramId, title, description, position, null, null);
    }

    public ModuleEntity(
            UUID id,
            UUID learningProgramId,
            String title,
            String description,
            int position,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.learningProgramId = Objects.requireNonNull(learningProgramId);
        this.title = Objects.requireNonNull(title);
        this.description = description;
        if (position < 0) {
            throw new IllegalArgumentException("position must be greater than or equal to 0");
        }
        this.position = position;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getLearningProgramId() { return learningProgramId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getPosition() { return position; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
