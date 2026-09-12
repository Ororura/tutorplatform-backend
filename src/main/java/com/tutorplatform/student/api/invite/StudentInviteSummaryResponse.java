package com.tutorplatform.student.api.invite;

import com.tutorplatform.student.domain.StudentInviteStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record StudentInviteSummaryResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "email") String email,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) StudentInviteStatus status,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant expiresAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant createdAt
) {
}
