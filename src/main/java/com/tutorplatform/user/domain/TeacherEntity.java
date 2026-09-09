package com.tutorplatform.user.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TeacherEntity(UUID id, UUID userId, String displayName, Instant createdAt, Instant updatedAt) {

    public TeacherEntity(UUID id, UserEntity user, String displayName) {
        this(id, Objects.requireNonNull(user).id(), displayName, null, null);
    }

    public TeacherEntity(UUID id, UUID userId, String displayName, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.displayName = Objects.requireNonNull(displayName);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
