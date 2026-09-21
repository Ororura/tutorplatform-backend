package com.tutorplatform.user.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record UserEntity(
        UUID id,
        String email,
        String passwordHash,
        UserStatus status,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt,
        Set<UserRole> roles) {

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
            Set<UserRole> roles) {
        this.id = Objects.requireNonNull(id);
        this.email = Objects.requireNonNull(email);
        this.passwordHash = passwordHash;
        this.status = Objects.requireNonNull(status);
        this.lastLoginAt = lastLoginAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.roles = new HashSet<>(Objects.requireNonNull(roles));
    }

    @Override
    public Set<UserRole> roles() {
        return Set.copyOf(roles);
    }

    public void addRole(UserRole role) {
        roles.add(Objects.requireNonNull(role));
    }
}
