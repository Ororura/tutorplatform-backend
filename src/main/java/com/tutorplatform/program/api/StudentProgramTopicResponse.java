package com.tutorplatform.program.api;

import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

public record StudentProgramTopicResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
    @Schema(nullable = true) String description,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID moduleId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String moduleTitle,
    @Schema(nullable = true) StudentTopicProgressStatus progressStatus,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<StudentLessonMaterialResponse> materials
) {
}
