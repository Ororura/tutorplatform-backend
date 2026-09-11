package com.tutorplatform.report.infrastructure.persistence;

import com.tutorplatform.report.domain.ReportShare;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "report_shares")
public class ReportShareDatabaseModel {

    @Id
    private UUID id;

    @Column(name = "report_id", nullable = false)
    private UUID reportId;

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

    protected ReportShareDatabaseModel() {
    }

    ReportShareDatabaseModel(ReportShare share) {
        id = share.id();
        reportId = share.reportId();
        createdByTeacherId = share.createdByTeacherId();
        tokenHash = share.tokenHash();
        expiresAt = share.expiresAt();
        revokedAt = share.revokedAt();
        createdAt = share.createdAt();
    }

    ReportShare toDomain() {
        return new ReportShare(
            id, reportId, createdByTeacherId, tokenHash, expiresAt, revokedAt, createdAt
        );
    }
}
