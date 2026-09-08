package com.tutorplatform.task.api.response;

import com.tutorplatform.task.application.TaskResult;
import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskStatus;
import com.tutorplatform.task.domain.task.TaskType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID subjectId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 220) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String descriptionMarkdown,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"TEXT"}) TaskType taskType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaskDifficulty difficulty,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaskStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long version,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant updatedAt
) {
    public static TaskResponse from(TaskResult task) {
        return new TaskResponse(
                task.id(), task.subjectId(), task.title(), task.descriptionMarkdown(),
                task.taskType(), task.difficulty(), task.status(), task.version(),
                task.createdAt(), task.updatedAt()
        );
    }
}
