package com.tutorplatform.task.api.student;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tutorplatform.task.application.StudentTopicTaskResult;
import com.tutorplatform.task.domain.programming.ComparisonMode;
import com.tutorplatform.task.domain.programming.ProgrammingLanguage;
import com.tutorplatform.task.domain.programming.ProgrammingTaskConfig;
import com.tutorplatform.task.domain.programming.TaskTestCase;
import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudentTopicTaskResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String descriptionMarkdown,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        allowableValues = {"TEXT", "CODE"})
                TaskType taskType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaskDifficulty difficulty,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int position,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean required,
        ProgrammingConfigResponse programmingConfig,
        List<PublicTestCaseResponse> testCases) {
    public static StudentTopicTaskResponse from(StudentTopicTaskResult task) {
        return new StudentTopicTaskResponse(
                task.id(),
                task.title(),
                task.descriptionMarkdown(),
                task.taskType(),
                task.difficulty(),
                task.position(),
                task.required(),
                task.programmingConfig() == null
                        ? null
                        : ProgrammingConfigResponse.from(task.programmingConfig()),
                task.testCases() == null
                        ? null
                        : task.testCases().stream().map(PublicTestCaseResponse::from).toList());
    }

    public record ProgrammingConfigResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProgrammingLanguage language,
            @Schema(nullable = true) String starterCode,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean executionEnabled,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "100", maximum = "30000")
                    int timeLimitMs,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "16", maximum = "1024")
                    int memoryLimitMb) {
        static ProgrammingConfigResponse from(ProgrammingTaskConfig config) {
            return new ProgrammingConfigResponse(
                    config.language(),
                    config.starterCode(),
                    config.executionEnabled(),
                    config.timeLimitMs(),
                    config.memoryLimitMb());
        }
    }

    public record PublicTestCaseResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
            @Schema(nullable = true) String inputText,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String expectedOutput,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ComparisonMode comparisonMode,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int position) {
        static PublicTestCaseResponse from(TaskTestCase testCase) {
            return new PublicTestCaseResponse(
                    testCase.id(),
                    testCase.inputText(),
                    testCase.expectedOutput(),
                    testCase.comparisonMode(),
                    testCase.position());
        }
    }
}
