package com.tutorplatform.task.api.topic;

import com.tutorplatform.task.application.TopicTaskDetailsResult;
import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskStatus;
import com.tutorplatform.task.domain.task.TaskType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record TopicTaskDetailsResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID taskId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaskType taskType,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaskDifficulty difficulty,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaskStatus status,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int position,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean required
) {
    public static TopicTaskDetailsResponse from(TopicTaskDetailsResult task) {
        return new TopicTaskDetailsResponse(
            task.taskId(), task.title(), task.taskType(), task.difficulty(), task.status(),
            task.position(), task.required()
        );
    }
}
