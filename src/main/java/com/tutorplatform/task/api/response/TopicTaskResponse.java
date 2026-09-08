package com.tutorplatform.task.api.response;

import com.tutorplatform.task.application.TopicTaskResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record TopicTaskResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID topicId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID taskId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int position,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean required,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant createdAt
) {
    public static TopicTaskResponse from(TopicTaskResult topicTask) {
        return new TopicTaskResponse(
                topicTask.topicId(), topicTask.taskId(), topicTask.position(),
                topicTask.required(), topicTask.createdAt()
        );
    }
}
