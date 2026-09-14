package com.tutorplatform.program.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateLearningProgramRequest(
    @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID subjectId,
    @NotBlank @Size(max = 200) @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200) String title,
    @Schema(nullable = true) String description
) {
    public CreateLearningProgramRequest {
        title = title == null ? null : title.strip();
        description = description == null ? null : description.strip();
    }
}
