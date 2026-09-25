package com.tutorplatform.report.api.response;

import com.tutorplatform.report.application.LearningPeriodSummary;
import com.tutorplatform.report.domain.LearningPeriodStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "LearningPeriodResponse")
public record LearningPeriodResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1") int sequenceNo,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LearningPeriodStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
                int startCumulativeMinutes,
        @Schema(nullable = true, minimum = "0") Integer endCumulativeMinutes,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
                int targetDurationMinutes,
        @Schema(nullable = true, format = "date-time") Instant startedAt,
        @Schema(nullable = true, format = "date-time") Instant completedAt,
        @Schema(nullable = true, format = "uuid") UUID reportId) {

    public static LearningPeriodResponse from(LearningPeriodSummary period) {
        return new LearningPeriodResponse(
                period.id(),
                period.sequenceNo(),
                period.status(),
                period.startCumulativeMinutes(),
                period.endCumulativeMinutes(),
                period.targetDurationMinutes(),
                period.startedAt(),
                period.completedAt(),
                period.reportId());
    }
}
