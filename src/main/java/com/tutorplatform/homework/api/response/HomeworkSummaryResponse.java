package com.tutorplatform.homework.api.response;

import com.tutorplatform.homework.application.HomeworkSummaryResult;
import com.tutorplatform.homework.domain.HomeworkStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record HomeworkSummaryResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID studentProgramId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) HomeworkStatus status,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant assignedAt,
    @Schema(nullable = true, format = "date-time") Instant dueAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean overdue,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant createdAt
) {
    public static HomeworkSummaryResponse from(HomeworkSummaryResult homework) {
        return new HomeworkSummaryResponse(
            homework.id(), homework.studentProgramId(), homework.title(), homework.status(),
            homework.assignedAt(), homework.dueAt(), homework.overdue(), homework.createdAt()
        );
    }
}
