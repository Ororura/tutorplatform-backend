package com.tutorplatform.report.domain;

import java.util.List;

public record ProgressReportPage(List<ProgressReport> items, long totalElements, int totalPages) {
    public ProgressReportPage {
        items = List.copyOf(items);
    }
}
