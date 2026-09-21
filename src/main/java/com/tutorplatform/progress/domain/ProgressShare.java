package com.tutorplatform.progress.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ProgressShare(
        UUID id,
        UUID studentProgramId,
        UUID createdByTeacherId,
        String tokenHash,
        Instant expiresAt,
        Instant revokedAt,
        Instant createdAt) {
    public ProgressShare {
        Objects.requireNonNull(id);
        Objects.requireNonNull(studentProgramId);
        Objects.requireNonNull(createdByTeacherId);
        Objects.requireNonNull(tokenHash);
    }

    public ProgressShare revoke(Instant now) {
        return revokedAt == null
                ? new ProgressShare(
                        id,
                        studentProgramId,
                        createdByTeacherId,
                        tokenHash,
                        expiresAt,
                        Objects.requireNonNull(now),
                        createdAt)
                : this;
    }
}
