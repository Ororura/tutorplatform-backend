package com.tutorplatform.progress.domain;

public record SessionMetrics(
    long totalLearningMinutes,
    long attendedCount,
    long missedCount,
    long sessionsCount
) {
    public SessionMetrics {
        if (totalLearningMinutes < 0 || attendedCount < 0 || missedCount < 0 || sessionsCount < 0) {
            throw new IllegalArgumentException("Session metrics cannot be negative");
        }
    }
}
