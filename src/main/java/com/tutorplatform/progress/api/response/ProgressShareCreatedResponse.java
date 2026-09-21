package com.tutorplatform.progress.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "New progress share; the URL is returned only once")
public record ProgressShareCreatedResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID studentProgramId,
        @Schema(
                        types = {"string", "null"},
                        format = "date-time")
                Instant expiresAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uri") String shareUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
                Instant createdAt) {}
