package com.tutorplatform.homework.api.response;

import com.tutorplatform.homework.application.StudentHomeworkDetailsResult;
import com.tutorplatform.homework.domain.HomeworkStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudentHomeworkDetailsResponse(
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
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
                List<StudentHomeworkItemResponse> items) {
    public static StudentHomeworkDetailsResponse from(StudentHomeworkDetailsResult homework) {
        return new StudentHomeworkDetailsResponse(
                homework.id(),
                homework.studentProgramId(),
                homework.title(),
                homework.description(),
                homework.status(),
                homework.assignedAt(),
                homework.dueAt(),
                homework.overdue(),
                homework.completedAt(),
                homework.items().stream().map(StudentHomeworkItemResponse::from).toList());
    }
}
