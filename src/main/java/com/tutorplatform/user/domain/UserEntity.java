package com.tutorplatform.user.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class UserEntity {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final UserStatus status;
    private final Instant lastLoginAt;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Set<UserRole> roles;

    public UserEntity(UUID id, String email, String passwordHash, UserStatus status) {
        this(id, email, passwordHash, status, null, null, null, Set.of());
    }

    public UserEntity(
            UUID id,
            String email,
            String passwordHash,
            UserStatus status,
            Instant lastLoginAt,
            Instant createdAt,
            Instant updatedAt,
            Set<UserRole> roles
    ) {
        this.id = Objects.requireNonNull(id);
        this.email = Objects.requireNonNull(email);
        this.passwordHash = passwordHash;
        this.status = Objects.requireNonNull(status);
        this.lastLoginAt = lastLoginAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.roles = new HashSet<>(Objects.requireNonNull(roles));
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public UserStatus getStatus() { return status; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Set<UserRole> getRoles() { return Set.copyOf(roles); }

    public void addRole(UserRole role) {
        roles.add(Objects.requireNonNull(role));
    }
}
