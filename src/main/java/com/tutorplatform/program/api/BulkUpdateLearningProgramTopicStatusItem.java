package com.tutorplatform.program.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

public record BulkUpdateLearningProgramTopicStatusItem(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
        @NotNull @PositiveOrZero @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
                Long version) {}
