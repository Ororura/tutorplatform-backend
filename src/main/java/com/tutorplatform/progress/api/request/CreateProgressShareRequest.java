package com.tutorplatform.progress.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreateProgressShareRequest(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid")
                UUID studentProgramId,
        @Future
                @Schema(
                        types = {"string", "null"},
                        format = "date-time")
                Instant expiresAt) {}
