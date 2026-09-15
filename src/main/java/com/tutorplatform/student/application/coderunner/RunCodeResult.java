package com.tutorplatform.student.application.coderunner;

import com.tutorplatform.execution.application.ExecutionStatus;

import java.util.List;
import java.util.UUID;

public record RunCodeResult(
    UUID executionId,
    ExecutionStatus status,
    int passedTests,
    int totalTests,
    long executionTimeMs,
    String stdoutExcerpt,
    String stderrExcerpt,
    List<TestResult> tests
) {
    public record TestResult(
        int position,
        boolean hidden,
        boolean passed
    ) {
    }
}
