package com.tutorplatform.report.application.exception;

import java.util.UUID;

public class HistoricalLearningPeriodChangeException extends RuntimeException {
    public HistoricalLearningPeriodChangeException(UUID studentProgramId) {
        super(
                "Session history no longer matches completed LearningPeriods for StudentProgram "
                        + studentProgramId);
    }
}
