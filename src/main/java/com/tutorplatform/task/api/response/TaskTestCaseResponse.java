package com.tutorplatform.task.api.response;

import com.tutorplatform.task.domain.programming.ComparisonMode;
import com.tutorplatform.task.domain.programming.TaskTestCase;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record TaskTestCaseResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
    String inputText,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String expectedOutput,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean hidden,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ComparisonMode comparisonMode,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int position,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant createdAt
) {
    public static TaskTestCaseResponse from(TaskTestCase testCase) {
        return new TaskTestCaseResponse(testCase.id(), testCase.inputText(), testCase.expectedOutput(),
            testCase.hidden(), testCase.comparisonMode(), testCase.position(), testCase.createdAt());
    }
}
