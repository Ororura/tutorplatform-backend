package com.tutorplatform.task.api.programming;

import com.tutorplatform.task.domain.programming.ProgrammingLanguage;
import com.tutorplatform.task.domain.programming.ProgrammingTaskConfig;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record ProgrammingTaskConfigResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProgrammingLanguage language,
    String starterCode,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean executionEnabled,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "100", maximum = "30000") int timeLimitMs,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "16", maximum = "1024") int memoryLimitMb,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant createdAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant updatedAt
) {
    public static ProgrammingTaskConfigResponse from(ProgrammingTaskConfig config) {
        return new ProgrammingTaskConfigResponse(config.language(), config.starterCode(),
            config.executionEnabled(), config.timeLimitMs(), config.memoryLimitMb(),
            config.createdAt(), config.updatedAt());
    }
}
