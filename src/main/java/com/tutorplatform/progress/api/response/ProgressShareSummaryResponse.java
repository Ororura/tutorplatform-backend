package com.tutorplatform.progress.api.response;

import com.tutorplatform.progress.domain.ProgressShareStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record ProgressShareSummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID studentProgramId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProgressShareStatus status,
        @Schema(
                        types = {"string", "null"},
                        format = "date-time")
                Instant expiresAt,
        @Schema(
                        types = {"string", "null"},
                        format = "date-time")
                Instant revokedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
                Instant createdAt) {}
