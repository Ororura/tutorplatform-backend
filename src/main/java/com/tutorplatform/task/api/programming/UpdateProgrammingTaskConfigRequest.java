package com.tutorplatform.task.api.programming;

import com.tutorplatform.task.domain.programming.ProgrammingLanguage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateProgrammingTaskConfigRequest(
        ProgrammingLanguage language,
        String starterCode,
        @NotNull Boolean executionEnabled,
        @NotNull @Min(100) @Max(30000) Integer timeLimitMs,
        @NotNull @Min(16) @Max(1024) Integer memoryLimitMb) {}
