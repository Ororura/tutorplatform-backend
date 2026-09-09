package com.tutorplatform.student.infrastructure.persistence;

import com.tutorplatform.student.domain.StudentEntity;
import com.tutorplatform.user.domain.TeacherEntity;
import com.tutorplatform.user.infrastructure.persistence.TeacherDatabaseModel;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "student_invites")
public class StudentInviteEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, insertable = false, updatable = false)
    private StudentDatabaseModel student;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_teacher_id", nullable = false, insertable = false, updatable = false)
    private TeacherDatabaseModel createdByTeacher;

    @Column(name = "created_by_teacher_id", nullable = false)
    private UUID createdByTeacherId;

    @Column(nullable = false, columnDefinition = "citext")
    private String email;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StudentInviteEntity() {
    }

    public StudentInviteEntity(
            UUID id,
            StudentEntity student,
            TeacherEntity createdByTeacher,
            String email,
            String tokenHash,
            Instant expiresAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.studentId = Objects.requireNonNull(student).getId();
        this.createdByTeacherId = Objects.requireNonNull(createdByTeacher).id();
        this.email = Objects.requireNonNull(email);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.expiresAt = Objects.requireNonNull(expiresAt);
    }

    public UUID getId() {
        return id;
    }

    public StudentDatabaseModel getStudent() {
        return student;
    }

    public TeacherDatabaseModel getCreatedByTeacher() {
        return createdByTeacher;
    }

    public String getEmail() {
        return email;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void revoke(Instant revokedAt) {
        if (this.revokedAt == null) {
            this.revokedAt = Objects.requireNonNull(revokedAt);
        }
    }

    public void accept(Instant acceptedAt) {
        this.acceptedAt = Objects.requireNonNull(acceptedAt);
    }
}
