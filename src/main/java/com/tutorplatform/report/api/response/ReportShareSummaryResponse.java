package com.tutorplatform.report.api.response;

import com.tutorplatform.report.domain.ReportShareStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "ReportShareSummaryResponse")
public record ReportShareSummaryResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID reportId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ReportShareStatus status,
    @Schema(types = {"string", "null"}, format = "date-time") Instant expiresAt,
    @Schema(types = {"string", "null"}, format = "date-time") Instant revokedAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant createdAt
) {
}
