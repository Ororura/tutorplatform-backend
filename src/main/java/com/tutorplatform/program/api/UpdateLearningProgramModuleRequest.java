package com.tutorplatform.program.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLearningProgramModuleRequest(
        @NotBlank
                @Size(max = 180)
                @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 180)
                String title,
        @Schema(nullable = true) String description) {
    public UpdateLearningProgramModuleRequest {
        title = title == null ? null : title.strip();
        description = description == null ? null : description.strip();
    }
}
