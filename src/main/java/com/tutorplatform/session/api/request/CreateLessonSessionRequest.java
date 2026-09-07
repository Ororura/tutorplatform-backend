package com.tutorplatform.session.api.request;

import com.tutorplatform.session.domain.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateLessonSessionRequest(
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid")
    UUID studentProgramId,

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
    Instant startedAt,

    @Min(1)
    @Max(600)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "600")
    int durationMinutes,

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    AttendanceStatus attendanceStatus,

    @Schema(nullable = true)
    String summary,

    @Schema(nullable = true)
    String privateNotes,

    @NotNull
    List<@NotNull @Valid LessonSessionTopicRequest> topics
) {
}
