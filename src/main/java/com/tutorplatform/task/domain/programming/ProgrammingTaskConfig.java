package com.tutorplatform.task.domain.programming;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ProgrammingTaskConfig(
        UUID taskId,
        ProgrammingLanguage language,
        String starterCode,
        boolean executionEnabled,
        int timeLimitMs,
        int memoryLimitMb,
        Instant createdAt,
        Instant updatedAt) {
    public ProgrammingTaskConfig {
        Objects.requireNonNull(taskId, "taskId is required");
        Objects.requireNonNull(language, "language is required");
        if (timeLimitMs < 100 || timeLimitMs > 30_000) {
            throw new IllegalArgumentException("timeLimitMs must be between 100 and 30000");
        }
        if (memoryLimitMb < 16 || memoryLimitMb > 1_024) {
            throw new IllegalArgumentException("memoryLimitMb must be between 16 and 1024");
        }
    }

    public ProgrammingTaskConfig(
            UUID taskId,
            ProgrammingLanguage language,
            String starterCode,
            boolean executionEnabled,
            int timeLimitMs,
            int memoryLimitMb) {
        this(
                taskId,
                language,
                starterCode,
                executionEnabled,
                timeLimitMs,
                memoryLimitMb,
                null,
                null);
    }
}
