package com.tutorplatform.task.api.request;

import com.tutorplatform.task.domain.programming.ProgrammingLanguage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProgrammingTaskConfigRequest(
    @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProgrammingLanguage language,
    String starterCode,
    @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Boolean executionEnabled,
    @NotNull @Min(100) @Max(30000) Integer timeLimitMs,
    @NotNull @Min(16) @Max(1024) Integer memoryLimitMb
) {
}
