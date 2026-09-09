package com.tutorplatform.task.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AttachTaskToTopicRequest(
    @PositiveOrZero
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
    int position,

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    Boolean required
) {
}
