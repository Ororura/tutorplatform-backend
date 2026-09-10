package com.tutorplatform.report.api.response;

import com.tutorplatform.report.domain.ProgressReport;
import com.tutorplatform.report.domain.ProgressReportSnapshotV1;
import com.tutorplatform.report.domain.ProgressReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "ProgressReportDetailsResponse")
public record ProgressReportDetailsResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID studentProgramId,
    @Schema(nullable = true, format = "uuid") UUID learningPeriodId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProgressReportStatus status,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant periodStartedAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant periodEndedAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int learningMinutes,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1") int snapshotSchemaVersion,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProgressReportSnapshotV1 snapshot,
    @Schema(nullable = true) String teacherSummary,
    @Schema(nullable = true) String nextPeriodPlan,
    @Schema(nullable = true, format = "date-time") Instant publishedAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long version,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant createdAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant updatedAt
) {
    public static ProgressReportDetailsResponse from(ProgressReport report) {
        return new ProgressReportDetailsResponse(
            report.id(), report.studentProgramId(), report.learningPeriodId(), report.status(),
            report.periodStartedAt(), report.periodEndedAt(), report.learningMinutes(),
            report.snapshotSchemaVersion(), report.snapshot(), report.teacherSummary(),
            report.nextPeriodPlan(), report.publishedAt(), report.version(),
            report.createdAt(), report.updatedAt()
        );
    }
}
