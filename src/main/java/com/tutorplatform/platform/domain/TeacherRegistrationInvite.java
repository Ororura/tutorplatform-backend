package com.tutorplatform.platform.domain;

import java.time.Instant;
import java.util.UUID;

public record TeacherRegistrationInvite(
    UUID id,
    UUID createdByAdminId,
    String email,
    String tokenHash,
    Instant expiresAt,
    Instant acceptedAt,
    Instant revokedAt,
    Instant createdAt
) {

    public boolean isActive(Instant now) {
        return acceptedAt == null
            && revokedAt == null
            && expiresAt.isAfter(now);
    }
}
