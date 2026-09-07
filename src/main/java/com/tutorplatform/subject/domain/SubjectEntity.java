package com.tutorplatform.subject.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class SubjectEntity {

    private final UUID id;
    private final UUID ownerTeacherId;
    private final String code;
    private final String name;
    private final String description;
    private final SubjectStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public SubjectEntity(
            UUID id,
            UUID ownerTeacherId,
            String code,
            String name,
            String description,
            SubjectStatus status
    ) {
        this(id, ownerTeacherId, code, name, description, status, null, null);
    }

    public SubjectEntity(
            UUID id,
            UUID ownerTeacherId,
            String code,
            String name,
            String description,
            SubjectStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.ownerTeacherId = ownerTeacherId;
        this.code = code;
        this.name = Objects.requireNonNull(name);
        this.description = description;
        this.status = Objects.requireNonNull(status);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getOwnerTeacherId() { return ownerTeacherId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public SubjectStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
