package com.tutorplatform.program.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TopicEntity(UUID id, UUID moduleId, String title, String description, int position, TopicStatus status,
                          Long version, Instant createdAt, Instant updatedAt) {

    public TopicEntity(
        UUID id,
        UUID moduleId,
        String title,
        String description,
        int position,
        TopicStatus status
    ) {
        this(id, moduleId, title, description, position, status, null, null, null);
    }

    public TopicEntity(
        UUID id,
        UUID moduleId,
        String title,
        String description,
        int position,
        TopicStatus status,
        Long version,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.moduleId = Objects.requireNonNull(moduleId);
        this.title = Objects.requireNonNull(title);
        this.description = description;
        if (position < 0) {
            throw new IllegalArgumentException("position must be greater than or equal to 0");
        }
        this.position = position;
        this.status = Objects.requireNonNull(status);
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
