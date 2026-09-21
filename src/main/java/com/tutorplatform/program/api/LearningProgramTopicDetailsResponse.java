package com.tutorplatform.program.api;

import com.tutorplatform.program.domain.TopicStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record LearningProgramTopicDetailsResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String slug,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(nullable = true) String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int position,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TopicStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long version) {}
