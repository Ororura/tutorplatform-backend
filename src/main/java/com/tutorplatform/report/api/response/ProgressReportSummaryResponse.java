package com.tutorplatform.report.api.response;

import com.tutorplatform.report.application.ProgressReportSummary;
import com.tutorplatform.report.domain.ProgressReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "ProgressReportSummaryResponse")
public record ProgressReportSummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID studentProgramId,
        @Schema(nullable = true, format = "uuid") UUID learningPeriodId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProgressReportStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
                Instant periodStartedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
                Instant periodEndedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int learningMinutes,
        @Schema(nullable = true, format = "date-time") Instant publishedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
                Instant createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
                Instant updatedAt) {
    public static ProgressReportSummaryResponse from(ProgressReportSummary report) {
        return new ProgressReportSummaryResponse(
                report.id(),
                report.studentProgramId(),
                report.learningPeriodId(),
                report.status(),
                report.periodStartedAt(),
                report.periodEndedAt(),
                report.learningMinutes(),
                report.publishedAt(),
                report.createdAt(),
                report.updatedAt());
    }
}
