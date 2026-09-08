package com.tutorplatform.homework.api.response;

import com.tutorplatform.homework.application.StudentHomeworkDetails;
import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record StudentTaskResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String descriptionMarkdown,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaskType taskType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaskDifficulty difficulty
) {
    public static StudentTaskResponse from(StudentHomeworkDetails.Task task) {
        return new StudentTaskResponse(
                task.id(), task.title(), task.descriptionMarkdown(), task.taskType(), task.difficulty()
        );
    }
}
