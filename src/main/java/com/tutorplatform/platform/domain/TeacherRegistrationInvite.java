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
        Instant createdAt) {

    public TeacherRegistrationInviteStatus status(Instant now) {
        if (acceptedAt != null) {
            return TeacherRegistrationInviteStatus.ACCEPTED;
        }

        if (revokedAt != null) {
            return TeacherRegistrationInviteStatus.REVOKED;
        }

        if (!expiresAt.isAfter(now)) {
            return TeacherRegistrationInviteStatus.EXPIRED;
        }

        return TeacherRegistrationInviteStatus.ACTIVE;
    }

    public boolean isActive(Instant now) {
        return acceptedAt == null && revokedAt == null && expiresAt.isAfter(now);
    }
}
