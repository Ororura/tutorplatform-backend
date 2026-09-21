package com.tutorplatform.program.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateLearningProgramRequest(
        @NotBlank
                @Size(min = 1, max = 200)
                @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 200)
                String title,
        @Schema(nullable = true) String description,
        @NotNull @PositiveOrZero @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
                Long version) {
    public UpdateLearningProgramRequest {
        title = title == null ? null : title.strip();
    }
}
