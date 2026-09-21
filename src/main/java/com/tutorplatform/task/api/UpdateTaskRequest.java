package com.tutorplatform.task.api;

import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateTaskRequest(
        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 220)
                String title,
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String descriptionMarkdown,
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaskDifficulty difficulty,
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaskStatus status,
        @NotNull @PositiveOrZero @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
                Long version) {}
