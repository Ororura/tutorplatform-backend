package com.tutorplatform.program.api;

import com.tutorplatform.program.domain.TopicStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateLearningProgramTopicRequest(
    @NotBlank @Size(max = 180) @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 180) String title,
    @Schema(nullable = true) String description,
    @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TopicStatus status,
    @NotNull @PositiveOrZero @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") Long version
) {
    public UpdateLearningProgramTopicRequest {
        title = title == null ? null : title.strip();
        description = description == null ? null : description.strip();
    }
}
