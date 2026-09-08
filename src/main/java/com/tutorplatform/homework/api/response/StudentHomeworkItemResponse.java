package com.tutorplatform.homework.api.response;

import com.tutorplatform.homework.application.StudentHomeworkDetails;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record StudentHomeworkItemResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID taskId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int position,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean required,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) StudentTaskResponse task
) {
    public static StudentHomeworkItemResponse from(StudentHomeworkDetails.Item item) {
        return new StudentHomeworkItemResponse(
                item.id(), item.taskId(), item.position(), item.required(), StudentTaskResponse.from(item.task())
        );
    }
}
