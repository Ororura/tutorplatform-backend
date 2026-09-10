package com.tutorplatform.report.application;

import com.tutorplatform.report.domain.ProgressReportStatus;

import java.time.Instant;
import java.util.UUID;

public record ProgressReportSummary(
    UUID id,
    UUID studentProgramId,
    UUID learningPeriodId,
    ProgressReportStatus status,
    Instant periodStartedAt,
    Instant periodEndedAt,
    int learningMinutes,
    Instant publishedAt,
    Instant createdAt,
    Instant updatedAt
) {
}
