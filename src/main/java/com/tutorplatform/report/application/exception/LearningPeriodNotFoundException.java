package com.tutorplatform.report.application.exception;

public class LearningPeriodNotFoundException extends RuntimeException {
    public LearningPeriodNotFoundException() {
        super("LearningPeriod not found");
    }
}
