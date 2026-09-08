package com.tutorplatform.homework.api.response;

import com.tutorplatform.homework.application.HomeworkPageResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record HomeworkPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<HomeworkSummaryResponse> items,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int page,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "100") int size,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long totalElements,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int totalPages
) {
    public static HomeworkPageResponse from(HomeworkPageResult result) {
        return new HomeworkPageResponse(
                result.items().stream().map(HomeworkSummaryResponse::from).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages()
        );
    }
}
