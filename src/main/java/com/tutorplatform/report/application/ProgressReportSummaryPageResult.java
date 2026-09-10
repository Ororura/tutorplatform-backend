package com.tutorplatform.report.application;

import java.util.List;

public record ProgressReportSummaryPageResult(
    List<ProgressReportSummary> items,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public ProgressReportSummaryPageResult {
        items = List.copyOf(items);
    }
}
