package com.tutorplatform.homework.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateHomeworkRequest(
        @NotNull
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid")
        UUID studentProgramId,

        @NotBlank
        @Size(max = 220)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 220)
        String title,

        @Schema(nullable = true)
        String description,

        @Schema(nullable = true, format = "date-time")
        Instant dueAt,

        @NotEmpty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid HomeworkItemRequest> items
) {
}
