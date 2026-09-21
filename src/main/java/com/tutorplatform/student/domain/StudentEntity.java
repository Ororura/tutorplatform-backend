package com.tutorplatform.student.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class StudentEntity {

    private final UUID id;
    private final StudentStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;
    private UUID userId;
    private String firstName;
    private String lastName;

    public StudentEntity(UUID id, String firstName, String lastName, StudentStatus status) {
        this(id, null, firstName, lastName, status, null, null);
    }

    public StudentEntity(
            UUID id,
            UUID userId,
            String firstName,
            String lastName,
            StudentStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.userId = userId;
        this.firstName = Objects.requireNonNull(firstName);
        this.lastName = lastName;
        this.status = Objects.requireNonNull(status);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public StudentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateNames(String firstName, String lastName) {
        this.firstName = Objects.requireNonNull(firstName);
        this.lastName = lastName;
    }

    public void linkUser(UUID userId) {
        this.userId = Objects.requireNonNull(userId);
    }
}
