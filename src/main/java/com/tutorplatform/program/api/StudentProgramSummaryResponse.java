package com.tutorplatform.program.api;

import com.tutorplatform.program.domain.studentprogram.StudentProgramStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record StudentProgramSummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid")
                UUID learningProgramId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(nullable = true) String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) StudentProgramStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
                int reportIntervalMinutes,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
                Instant startedAt,
        @Schema(nullable = true, format = "date-time") Instant completedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProgramSubjectResponse subject) {}
