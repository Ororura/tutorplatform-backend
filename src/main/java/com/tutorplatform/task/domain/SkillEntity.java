package com.tutorplatform.task.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class SkillEntity {

    private final UUID id;
    private final UUID subjectId;
    private final String code;
    private final String name;
    private final String description;
    private final Instant createdAt;
    private final Instant updatedAt;

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

    public UUID getId() { return id; }
    public UUID getSubjectId() { return subjectId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
