package com.tutorplatform.homework.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tutorplatform.homework.application.StudentHomeworkDetails;
import com.tutorplatform.task.domain.programming.ProgrammingLanguage;
import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudentTaskResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String descriptionMarkdown,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaskType taskType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaskDifficulty difficulty,
        @Schema(nullable = true) CodeExecutionResponse codeExecution) {
    public static StudentTaskResponse from(StudentHomeworkDetails.Task task) {
        return new StudentTaskResponse(
                task.id(),
                task.title(),
                task.descriptionMarkdown(),
                task.taskType(),
                task.difficulty(),
                task.codeExecution() == null
                        ? null
                        : CodeExecutionResponse.from(task.codeExecution()));
    }

    public record CodeExecutionResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProgrammingLanguage language,
            @Schema(nullable = true) String starterCode,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean executionEnabled,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "100", maximum = "30000")
                    int timeLimitMs,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "16", maximum = "1024")
                    int memoryLimitMb) {
        static CodeExecutionResponse from(StudentHomeworkDetails.CodeExecution execution) {
            return new CodeExecutionResponse(
                    execution.language(),
                    execution.starterCode(),
                    execution.executionEnabled(),
                    execution.timeLimitMs(),
                    execution.memoryLimitMb());
        }
    }
}
