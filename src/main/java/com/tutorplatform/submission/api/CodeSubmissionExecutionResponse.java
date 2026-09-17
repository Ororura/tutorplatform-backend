package com.tutorplatform.submission.api;

import com.tutorplatform.submission.application.CodeSubmissionResult;
import com.tutorplatform.submission.domain.CodeExecutionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record CodeSubmissionExecutionResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CodeExecutionStatus status,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int passedTests,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int totalTests,
    @Schema(nullable = true, minimum = "0") Integer executionTimeMs,
    @Schema(nullable = true) String stdoutExcerpt,
    @Schema(nullable = true) String stderrExcerpt
) {
    static CodeSubmissionExecutionResponse from(CodeSubmissionResult result) {
        return new CodeSubmissionExecutionResponse(
            result.status(), result.passedTests(), result.totalTests(), result.executionTimeMs(),
            result.stdoutExcerpt(), result.stderrExcerpt()
        );
    }
}
