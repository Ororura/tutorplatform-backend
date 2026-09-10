package com.tutorplatform.report.application;

import com.tutorplatform.report.domain.ProgressReport;

import java.util.List;

public record ProgressReportPageResult(
    List<ProgressReport> items,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public ProgressReportPageResult {
        items = List.copyOf(items);
    }
}
