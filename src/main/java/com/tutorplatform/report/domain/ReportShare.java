package com.tutorplatform.report.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ReportShare(
    UUID id,
    UUID reportId,
    UUID createdByTeacherId,
    String tokenHash,
    Instant expiresAt,
    Instant revokedAt,
    Instant createdAt
) {
    public ReportShare {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(reportId, "reportId");
        Objects.requireNonNull(createdByTeacherId, "createdByTeacherId");
        Objects.requireNonNull(tokenHash, "tokenHash");
    }

    public ReportShare revoke(Instant now) {
        return revokedAt == null
            ? new ReportShare(id, reportId, createdByTeacherId, tokenHash, expiresAt,
                Objects.requireNonNull(now, "now"), createdAt)
            : this;
    }

    public ReportShareStatus statusAt(Instant now) {
        if (revokedAt != null) {
            return ReportShareStatus.REVOKED;
        }
        if (expiresAt != null && expiresAt.isBefore(now)) {
            return ReportShareStatus.EXPIRED;
        }
        return ReportShareStatus.ACTIVE;
    }
}
