package com.tutorplatform.student.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tutorplatform.student.application.RunCodeResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "RunCodeResponse", description = "Transient student-safe execution result")
public record RunCodeResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID executionId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RunCodeExecutionStatus status,
    int passedTests,
    int totalTests,
    long executionTimeMs,
    String stdoutExcerpt,
    String stderrExcerpt,
    List<RunCodeTestResponse> tests
) {
    public static RunCodeResponse from(RunCodeResult result) {
        return new RunCodeResponse(
            result.executionId(), RunCodeExecutionStatus.valueOf(result.status().name()),
            result.passedTests(), result.totalTests(),
            result.executionTimeMs(), result.stdoutExcerpt(), result.stderrExcerpt(),
            result.tests().stream().map(RunCodeTestResponse::from).toList()
        );
    }
}
