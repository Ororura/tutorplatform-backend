package com.tutorplatform.task.api.request;

import com.tutorplatform.task.domain.task.TaskDifficulty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateTaskRequest(
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid")
    UUID subjectId,

    @NotBlank
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 220)
    String title,

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String descriptionMarkdown,

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    TaskDifficulty difficulty
) {
}
