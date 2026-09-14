package com.tutorplatform.program.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record AssignStudentProgramRequest(
    @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID learningProgramId,
    @Positive @Schema(nullable = true, minimum = "1", defaultValue = "480") Integer reportIntervalMinutes
) {
}
