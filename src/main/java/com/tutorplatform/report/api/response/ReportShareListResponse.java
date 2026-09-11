package com.tutorplatform.report.api.response;

import java.util.List;

public record ReportShareListResponse(List<ReportShareSummaryResponse> items) {
    public ReportShareListResponse {
        items = List.copyOf(items);
    }
}
