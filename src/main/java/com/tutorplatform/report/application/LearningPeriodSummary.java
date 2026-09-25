package com.tutorplatform.report.application;

import com.tutorplatform.report.domain.LearningPeriod;
import com.tutorplatform.report.domain.LearningPeriodStatus;
import java.time.Instant;
import java.util.UUID;

public record LearningPeriodSummary(
        UUID id,
        int sequenceNo,
        LearningPeriodStatus status,
        int startCumulativeMinutes,
        Integer endCumulativeMinutes,
        int targetDurationMinutes,
        Instant startedAt,
        Instant completedAt,
        UUID reportId) {

    static LearningPeriodSummary from(LearningPeriod period, UUID reportId) {
        return new LearningPeriodSummary(
                period.id(),
                period.sequenceNo(),
                period.status(),
                period.startCumulativeMinutes(),
                period.endCumulativeMinutes(),
                period.targetDurationMinutes(),
                period.startedAt(),
                period.completedAt(),
                reportId);
    }
}
