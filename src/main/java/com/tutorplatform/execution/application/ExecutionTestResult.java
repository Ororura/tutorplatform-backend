package com.tutorplatform.execution.application;

import java.util.Objects;
import java.util.UUID;

public record ExecutionTestResult(
    UUID testCaseId,
    boolean passed,
    long executionTimeMs,
    String stdoutExcerpt,
    String stderrExcerpt
) {
    public ExecutionTestResult {
        Objects.requireNonNull(testCaseId, "testCaseId must not be null");
        if (executionTimeMs < 0) {
            throw new IllegalArgumentException("executionTimeMs must not be negative");
        }
    }
}
