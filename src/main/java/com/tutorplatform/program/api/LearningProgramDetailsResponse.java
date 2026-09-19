package com.tutorplatform.program.api;

import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LearningProgramDetailsResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProgramSubjectResponse subject,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
    @Schema(nullable = true) String description,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LearningProgramStatus status,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long version,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant createdAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant updatedAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean hasAssignments,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean editable,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<LearningProgramModuleDetailsResponse> modules
) {
}
