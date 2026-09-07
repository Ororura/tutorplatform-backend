package com.tutorplatform.user.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class TeacherEntity {

    private final UUID id;
    private final UUID userId;
    private final String displayName;
    private final Instant createdAt;
    private final Instant updatedAt;

    public TeacherEntity(UUID id, UserEntity user, String displayName) {
        this(id, Objects.requireNonNull(user).getId(), displayName, null, null);
    }

    public TeacherEntity(UUID id, UUID userId, String displayName, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.displayName = Objects.requireNonNull(displayName);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
