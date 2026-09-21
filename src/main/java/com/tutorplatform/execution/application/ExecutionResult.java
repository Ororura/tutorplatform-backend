package com.tutorplatform.execution.application;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ExecutionResult(
        UUID executionId,
        ExecutionStatus status,
        int passedTests,
        int totalTests,
        long executionTimeMs,
        String stdoutExcerpt,
        String stderrExcerpt,
        List<ExecutionTestResult> testResults) {
    public ExecutionResult {
        Objects.requireNonNull(executionId, "executionId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(testResults, "testResults must not be null");
        if (passedTests < 0 || totalTests < 0 || passedTests > totalTests) {
            throw new IllegalArgumentException("test counts are invalid");
        }
        if (executionTimeMs < 0) {
            throw new IllegalArgumentException("executionTimeMs must not be negative");
        }
        testResults = List.copyOf(testResults);
    }

    public static ExecutionResult systemError(UUID executionId) {
        return systemError(executionId, 0);
    }

    public static ExecutionResult systemError(UUID executionId, int totalTests) {
        return new ExecutionResult(
                executionId, ExecutionStatus.SYSTEM_ERROR, 0, totalTests, 0, null, null, List.of());
    }
}
