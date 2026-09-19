package com.tutorplatform.program.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLearningProgramModuleRequest(
    @NotBlank @Size(max = 180) @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 180) String title,
    @Schema(nullable = true) String description
) {
    public CreateLearningProgramModuleRequest {
        title = title == null ? null : title.strip();
        description = description == null ? null : description.strip();
    }
}
