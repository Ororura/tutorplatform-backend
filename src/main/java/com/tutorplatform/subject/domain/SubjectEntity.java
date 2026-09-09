package com.tutorplatform.subject.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SubjectEntity(UUID id, UUID ownerTeacherId, String code, String name, String description,
                            SubjectStatus status, Instant createdAt, Instant updatedAt) {

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
}
