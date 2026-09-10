package com.tutorplatform.progress.domain;

public record PracticeMetrics(long assigned, long completed) {
    public PracticeMetrics {
        if (assigned < 0 || completed < 0 || completed > assigned) {
            throw new IllegalArgumentException("Invalid practice metrics");
        }
    }
}
