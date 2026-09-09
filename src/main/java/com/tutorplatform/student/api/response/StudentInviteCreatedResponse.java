package com.tutorplatform.student.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record StudentInviteCreatedResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID studentId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "email") String email,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant expiresAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uri") String inviteUrl,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant createdAt
) {
}
