package com.tutorplatform.homework.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

public record UpdateHomeworkRequest(
        @NotBlank
        @Size(max = 220)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 220)
        String title,

        @Schema(nullable = true)
        String description,

        @Schema(nullable = true, format = "date-time")
        Instant dueAt,

        @NotNull
        @PositiveOrZero
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
        Long version,

        @NotEmpty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid HomeworkItemRequest> items
) {
}
