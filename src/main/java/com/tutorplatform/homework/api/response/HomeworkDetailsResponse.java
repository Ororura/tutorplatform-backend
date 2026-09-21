package com.tutorplatform.homework.api.response;

import com.tutorplatform.homework.application.HomeworkResult;
import com.tutorplatform.homework.domain.HomeworkStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HomeworkDetailsResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID studentProgramId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(nullable = true) String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) HomeworkStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
                Instant assignedAt,
        @Schema(nullable = true, format = "date-time") Instant dueAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean overdue,
        @Schema(nullable = true, format = "date-time") Instant completedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<HomeworkItemResponse> items,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long version,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
                Instant createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
                Instant updatedAt) {
    public static HomeworkDetailsResponse from(HomeworkResult homework) {
        return new HomeworkDetailsResponse(
                homework.id(),
                homework.studentProgramId(),
                homework.title(),
                homework.description(),
                homework.status(),
                homework.assignedAt(),
                homework.dueAt(),
                homework.overdue(),
                homework.completedAt(),
                homework.items().stream().map(HomeworkItemResponse::from).toList(),
                homework.version(),
                homework.createdAt(),
                homework.updatedAt());
    }
}
