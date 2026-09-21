package com.tutorplatform.homework.api.response;

import com.tutorplatform.homework.application.HomeworkItemResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record HomeworkItemResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID taskId,
        @Schema(nullable = true) String taskTitle,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int position,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean required) {
    public static HomeworkItemResponse from(HomeworkItemResult item) {
        return new HomeworkItemResponse(
                item.id(), item.taskId(), item.taskTitle(), item.position(), item.required());
    }
}
