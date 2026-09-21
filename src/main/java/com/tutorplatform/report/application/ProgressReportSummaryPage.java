package com.tutorplatform.report.application;

import java.util.List;

public record ProgressReportSummaryPage(
        List<ProgressReportSummary> items, long totalElements, int totalPages) {
    public ProgressReportSummaryPage {
        items = List.copyOf(items);
    }
}
