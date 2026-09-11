package com.tutorplatform.report.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "ReportShareCreatedResponse", description = "New report share; the URL is returned only once")
public record ReportShareCreatedResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID reportId,
    @Schema(types = {"string", "null"}, format = "date-time") Instant expiresAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uri") String shareUrl,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant createdAt
) {
}
