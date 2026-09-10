package com.tutorplatform.progress.domain;

public record HomeworkMetrics(long assigned, long completed) {
    public HomeworkMetrics {
        if (assigned < 0 || completed < 0 || completed > assigned) {
            throw new IllegalArgumentException("Invalid homework metrics");
        }
    }
}
