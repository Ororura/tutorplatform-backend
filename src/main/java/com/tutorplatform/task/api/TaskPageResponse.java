package com.tutorplatform.task.api;

import com.tutorplatform.task.application.TaskPageResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record TaskPageResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<TaskResponse> items,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int page,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "100") int size,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long totalElements,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int totalPages
) {
    public static TaskPageResponse from(TaskPageResult result) {
        return new TaskPageResponse(
            result.items().stream().map(TaskResponse::from).toList(),
            result.page(), result.size(), result.totalElements(), result.totalPages()
        );
    }
}
