package com.tutorplatform.execution.application;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ExecutionRequest(
    UUID executionId,
    ExecutionLanguage language,
    String sourceCode,
    int timeLimitMs,
    int memoryLimitMb,
    List<ExecutionTestCase> testCases
) {
    public static final int MIN_TIME_LIMIT_MS = 100;
    public static final int MAX_TIME_LIMIT_MS = 30_000;
    public static final int MIN_MEMORY_LIMIT_MB = 16;
    public static final int MAX_MEMORY_LIMIT_MB = 1_024;

    public ExecutionRequest {
        Objects.requireNonNull(executionId, "executionId must not be null");
        Objects.requireNonNull(language, "language must not be null");
        Objects.requireNonNull(sourceCode, "sourceCode must not be null");
        Objects.requireNonNull(testCases, "testCases must not be null");
        if (sourceCode.isBlank()) {
            throw new IllegalArgumentException("sourceCode must not be blank");
        }
        if (timeLimitMs < MIN_TIME_LIMIT_MS || timeLimitMs > MAX_TIME_LIMIT_MS) {
            throw new IllegalArgumentException("timeLimitMs must be between 100 and 30000");
        }
        if (memoryLimitMb < MIN_MEMORY_LIMIT_MB || memoryLimitMb > MAX_MEMORY_LIMIT_MB) {
            throw new IllegalArgumentException("memoryLimitMb must be between 16 and 1024");
        }
        if (testCases.isEmpty()) {
            throw new IllegalArgumentException("testCases must not be empty");
        }
        testCases = List.copyOf(testCases);
    }
}
