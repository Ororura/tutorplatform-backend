package com.tutorplatform.program.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ModuleEntity(UUID id, UUID learningProgramId, String title, String description, int position,
                           Instant createdAt, Instant updatedAt) {

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
}
