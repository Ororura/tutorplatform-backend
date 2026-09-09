package com.tutorplatform.task.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tutorplatform.task.application.TaskResult;
import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskStatus;
import com.tutorplatform.task.domain.task.TaskType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TaskResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID subjectId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 220) String title,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String descriptionMarkdown,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"TEXT", "CODE"}) TaskType taskType,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaskDifficulty difficulty,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaskStatus status,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long version,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant createdAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant updatedAt,
    ProgrammingTaskConfigResponse programmingConfig,
    List<TaskTestCaseResponse> testCases
) {
    public static TaskResponse from(TaskResult task) {
        return new TaskResponse(
            task.id(), task.subjectId(), task.title(), task.descriptionMarkdown(),
            task.taskType(), task.difficulty(), task.status(), task.version(),
            task.createdAt(), task.updatedAt(),
            task.programmingConfig() == null ? null : ProgrammingTaskConfigResponse.from(task.programmingConfig()),
            task.testCases() == null ? null : task.testCases().stream().map(TaskTestCaseResponse::from).toList()
        );
    }
}
