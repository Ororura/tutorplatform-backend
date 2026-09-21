package com.tutorplatform.homework.api.response;

import com.tutorplatform.homework.application.StudentHomeworkSummaryResult;
import com.tutorplatform.homework.domain.HomeworkStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record StudentHomeworkSummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID studentProgramId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) HomeworkStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
                Instant assignedAt,
        @Schema(nullable = true, format = "date-time") Instant dueAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean overdue,
        @Schema(nullable = true, format = "date-time") Instant completedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long itemsCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
                Instant createdAt) {
    public static StudentHomeworkSummaryResponse from(StudentHomeworkSummaryResult homework) {
        return new StudentHomeworkSummaryResponse(
                homework.id(),
                homework.studentProgramId(),
                homework.title(),
                homework.status(),
                homework.assignedAt(),
                homework.dueAt(),
                homework.overdue(),
                homework.completedAt(),
                homework.itemsCount(),
                homework.createdAt());
    }
}
