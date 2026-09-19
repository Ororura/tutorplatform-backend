package com.tutorplatform.program.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

public record LearningProgramModuleDetailsResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
    @Schema(nullable = true) String description,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int position,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<LearningProgramTopicDetailsResponse> topics
) {
}
