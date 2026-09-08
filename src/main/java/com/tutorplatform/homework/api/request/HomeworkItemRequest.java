package com.tutorplatform.homework.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record HomeworkItemRequest(
        @NotNull
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid")
        UUID taskId,

        @PositiveOrZero
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
        int position,

        @NotNull
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean required
) {
}
