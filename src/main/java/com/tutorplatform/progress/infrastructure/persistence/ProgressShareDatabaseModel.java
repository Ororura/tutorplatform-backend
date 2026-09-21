package com.tutorplatform.progress.infrastructure.persistence;

import com.tutorplatform.progress.domain.ProgressShare;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "progress_shares")
public class ProgressShareDatabaseModel {

    @Id private UUID id;

    @Column(name = "student_program_id", nullable = false)
    private UUID studentProgramId;

    @Column(name = "created_by_teacher_id", nullable = false)
    private UUID createdByTeacherId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ProgressShareDatabaseModel() {}

    ProgressShareDatabaseModel(ProgressShare share) {
        id = share.id();
        studentProgramId = share.studentProgramId();
        createdByTeacherId = share.createdByTeacherId();
        tokenHash = share.tokenHash();
        expiresAt = share.expiresAt();
        revokedAt = share.revokedAt();
        createdAt = share.createdAt();
    }

    ProgressShare toDomain() {
        return new ProgressShare(
                id,
                studentProgramId,
                createdByTeacherId,
                tokenHash,
                expiresAt,
                revokedAt,
                createdAt);
    }
}
