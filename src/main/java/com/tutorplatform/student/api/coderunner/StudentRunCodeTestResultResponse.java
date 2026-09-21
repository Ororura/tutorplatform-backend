package com.tutorplatform.student.api.coderunner;

import com.tutorplatform.student.application.coderunner.RunCodeResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Student-safe test outcome without test content or process output")
public record StudentRunCodeTestResultResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int position,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean hidden,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean passed) {
    public static StudentRunCodeTestResultResponse from(RunCodeResult.TestResult result) {
        return new StudentRunCodeTestResultResponse(
                result.position(), result.hidden(), result.passed());
    }
}
