package com.tutorplatform.submission.application;

import com.tutorplatform.submission.domain.CodeExecutionStatus;
import com.tutorplatform.submission.domain.CodeSubmissionEntity;
import com.tutorplatform.submission.domain.CodeSubmissionRepository;

public record CodeSubmissionResult(
    CodeExecutionStatus status,
    int passedTests,
    int totalTests,
    Integer executionTimeMs,
    String stdoutExcerpt,
    String stderrExcerpt,
    String sourceCode
) {
    static CodeSubmissionResult from(CodeSubmissionEntity entity, boolean includeSourceCode) {
        return new CodeSubmissionResult(
            entity.getExecutionStatus(), entity.getPassedTests(), entity.getTotalTests(),
            entity.getExecutionTimeMs(), entity.getStdoutExcerpt(), entity.getStderrExcerpt(),
            includeSourceCode ? entity.getSourceCode() : null
        );
    }

    static CodeSubmissionResult from(CodeSubmissionRepository.Summary summary) {
        return new CodeSubmissionResult(
            summary.executionStatus(), summary.passedTests(), summary.totalTests(),
            summary.executionTimeMs(), summary.stdoutExcerpt(), summary.stderrExcerpt(), null
        );
    }
}
