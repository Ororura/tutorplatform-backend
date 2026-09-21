package com.tutorplatform.report.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record LearningPeriod(
        UUID id,
        UUID studentProgramId,
        int sequenceNo,
        int startCumulativeMinutes,
        int targetDurationMinutes,
        Integer endCumulativeMinutes,
        LearningPeriodStatus status,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt) {
    public LearningPeriod {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(studentProgramId, "studentProgramId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (sequenceNo <= 0) {
            throw new IllegalArgumentException("sequenceNo must be greater than 0");
        }
        if (startCumulativeMinutes < 0) {
            throw new IllegalArgumentException("startCumulativeMinutes must not be negative");
        }
        if (targetDurationMinutes <= 0) {
            throw new IllegalArgumentException("targetDurationMinutes must be greater than 0");
        }
        if (status == LearningPeriodStatus.ACTIVE
                && (endCumulativeMinutes != null || completedAt != null)) {
            throw new IllegalArgumentException("ACTIVE period cannot have completion values");
        }
        if (status == LearningPeriodStatus.COMPLETED) {
            if (endCumulativeMinutes == null || completedAt == null) {
                throw new IllegalArgumentException("COMPLETED period requires completion values");
            }
            if (endCumulativeMinutes < startCumulativeMinutes) {
                throw new IllegalArgumentException("endCumulativeMinutes precedes period start");
            }
        }
    }

    public static LearningPeriod active(
            UUID id,
            UUID studentProgramId,
            int sequenceNo,
            int startCumulativeMinutes,
            int targetDurationMinutes,
            Instant now) {
        return new LearningPeriod(
                id,
                studentProgramId,
                sequenceNo,
                startCumulativeMinutes,
                targetDurationMinutes,
                null,
                LearningPeriodStatus.ACTIVE,
                null,
                null,
                now,
                now);
    }

    public int thresholdMinutes() {
        return Math.addExact(startCumulativeMinutes, targetDurationMinutes);
    }

    public LearningPeriod withStartedAt(Instant firstAttendedAt, Instant now) {
        if (status != LearningPeriodStatus.ACTIVE || Objects.equals(startedAt, firstAttendedAt)) {
            return this;
        }
        return new LearningPeriod(
                id,
                studentProgramId,
                sequenceNo,
                startCumulativeMinutes,
                targetDurationMinutes,
                null,
                status,
                firstAttendedAt,
                null,
                createdAt,
                now);
    }

    public LearningPeriod complete(int cumulativeMinutes, Instant crossedAt, Instant now) {
        if (status == LearningPeriodStatus.COMPLETED) {
            return this;
        }
        if (cumulativeMinutes < thresholdMinutes()) {
            throw new IllegalArgumentException("period threshold has not been reached");
        }
        return new LearningPeriod(
                id,
                studentProgramId,
                sequenceNo,
                startCumulativeMinutes,
                targetDurationMinutes,
                cumulativeMinutes,
                LearningPeriodStatus.COMPLETED,
                startedAt,
                Objects.requireNonNull(crossedAt, "crossedAt"),
                createdAt,
                now);
    }
}
