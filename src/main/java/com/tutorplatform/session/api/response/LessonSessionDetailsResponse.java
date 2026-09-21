package com.tutorplatform.session.api.response;

import com.tutorplatform.session.application.LessonSessionResult;
import com.tutorplatform.session.domain.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LessonSessionDetailsResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID studentProgramId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
                Instant startedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "600")
                int durationMinutes,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AttendanceStatus attendanceStatus,
        @Schema(nullable = true) String summary,
        @Schema(nullable = true) String privateNotes,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
                List<LessonSessionTopicResponse> topics,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
                Instant createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
                Instant updatedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long version) {
    public static LessonSessionDetailsResponse from(LessonSessionResult session) {
        return new LessonSessionDetailsResponse(
                session.id(),
                session.studentProgramId(),
                session.startedAt(),
                session.durationMinutes(),
                session.attendanceStatus(),
                session.summary(),
                session.privateNotes(),
                session.topics().stream().map(LessonSessionTopicResponse::from).toList(),
                session.createdAt(),
                session.updatedAt(),
                session.version());
    }
}
