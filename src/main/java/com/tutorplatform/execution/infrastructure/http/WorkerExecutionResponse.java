package com.tutorplatform.execution.infrastructure.http;

import java.util.List;
import java.util.UUID;

record WorkerExecutionResponse(
        UUID executionId,
        String status,
        Integer passedTests,
        Integer totalTests,
        Long executionTimeMs,
        String stdoutExcerpt,
        String stderrExcerpt,
        List<TestResult> testResults) {
    record TestResult(
            UUID testCaseId,
            Boolean passed,
            Long executionTimeMs,
            String stdoutExcerpt,
            String stderrExcerpt) {}
}
