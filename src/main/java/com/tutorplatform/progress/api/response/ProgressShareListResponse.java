package com.tutorplatform.progress.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ProgressShareListResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ProgressShareSummaryResponse> items
) {
}
