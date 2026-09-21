package com.tutorplatform.report.application;

public record EditProgressReportDraftCommand(
        String teacherSummary,
        String nextPeriodPlan,
        long version,
        boolean teacherSummaryPresent,
        boolean nextPeriodPlanPresent) {
    public EditProgressReportDraftCommand(
            String teacherSummary, String nextPeriodPlan, long version) {
        this(teacherSummary, nextPeriodPlan, version, true, true);
    }

    public EditProgressReportDraftCommand {
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }
}
