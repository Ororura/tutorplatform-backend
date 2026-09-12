package com.tutorplatform.student.api.codeexecution;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tutorplatform.student.application.coderunner.RunCodeResult;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Student-safe test result. Hidden tests never expose input or expected output.")
public record RunCodeTestResponse(
    boolean passed,
    boolean hidden,
    String input,
    String expectedOutput,
    String actualOutput
) {
    public static RunCodeTestResponse from(RunCodeResult.TestResult result) {
        return new RunCodeTestResponse(
            result.passed(), result.hidden(), result.input(), result.expectedOutput(),
            result.actualOutput()
        );
    }
}
