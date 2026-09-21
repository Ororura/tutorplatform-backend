package com.tutorplatform.user.infrastructure.persistence;

import com.tutorplatform.user.domain.UserEntity;
import com.tutorplatform.user.domain.UserRole;
import com.tutorplatform.user.domain.UserStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "users")
public class UserDatabaseModel {
    @Id private UUID id;

    @Column(nullable = false, unique = true, columnDefinition = "citext")
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private UserStatus status;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private Set<UserRole> roles = new HashSet<>();

    protected UserDatabaseModel() {}

    UserDatabaseModel(UserEntity user) {
        this.id = user.id();
        updateFrom(user);
    }

    void updateFrom(UserEntity user) {
        email = user.email();
        passwordHash = user.passwordHash();
        status = user.status();
        lastLoginAt = user.lastLoginAt();
        roles.clear();
        roles.addAll(user.roles());
    }

    UserEntity toEntity() {
        return new UserEntity(
                id, email, passwordHash, status, lastLoginAt, createdAt, updatedAt, roles);
    }

    public UUID getId() {
        return id;
    }
}
