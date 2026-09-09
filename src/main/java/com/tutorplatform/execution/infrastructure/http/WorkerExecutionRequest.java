package com.tutorplatform.execution.infrastructure.http;

import com.tutorplatform.execution.application.ExecutionRequest;

import java.util.List;
import java.util.UUID;

record WorkerExecutionRequest(
    UUID executionId,
    String language,
    String sourceCode,
    int timeLimitMs,
    int memoryLimitMb,
    int outputLimitBytes,
    List<TestCase> testCases
) {
    static WorkerExecutionRequest from(ExecutionRequest request, int outputLimitBytes) {
        return new WorkerExecutionRequest(
            request.executionId(),
            request.language().name(),
            request.sourceCode(),
            request.timeLimitMs(),
            request.memoryLimitMb(),
            outputLimitBytes,
            request.testCases().stream()
                .map(testCase -> new TestCase(
                    testCase.id(),
                    testCase.inputText(),
                    testCase.expectedOutput(),
                    testCase.comparisonMode().name()
                ))
                .toList()
        );
    }

    record TestCase(UUID id, String inputText, String expectedOutput, String comparisonMode) {
    }
}
