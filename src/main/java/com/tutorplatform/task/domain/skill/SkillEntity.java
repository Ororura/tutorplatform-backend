package com.tutorplatform.task.domain.skill;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SkillEntity(UUID id, UUID subjectId, String code, String name, String description, Instant createdAt,
                          Instant updatedAt) {

    public SkillEntity(UUID id, UUID subjectId, String code, String name, String description) {
        this(id, subjectId, code, name, description, null, null);
    }

    public SkillEntity(
        UUID id,
        UUID subjectId,
        String code,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.subjectId = Objects.requireNonNull(subjectId);
        this.code = Objects.requireNonNull(code);
        this.name = Objects.requireNonNull(name);
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
