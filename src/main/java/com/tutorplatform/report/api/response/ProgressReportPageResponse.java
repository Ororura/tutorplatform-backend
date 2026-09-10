package com.tutorplatform.report.api.response;

import com.tutorplatform.report.application.ProgressReportSummaryPageResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ProgressReportPageResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ProgressReportSummaryResponse> items,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int page,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "100") int size,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long totalElements,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int totalPages
) {
    public static ProgressReportPageResponse from(ProgressReportSummaryPageResult result) {
        return new ProgressReportPageResponse(
            result.items().stream().map(ProgressReportSummaryResponse::from).toList(),
            result.page(), result.size(), result.totalElements(), result.totalPages()
        );
    }
}
