package com.tutorplatform.session.api.response;

import com.tutorplatform.session.application.LessonSessionPageResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record LessonSessionPageResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<LessonSessionSummaryResponse> items,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int page,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "100") int size,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long totalElements,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int totalPages
) {
    public static LessonSessionPageResponse from(LessonSessionPageResult result) {
        return new LessonSessionPageResponse(
            result.items().stream().map(LessonSessionSummaryResponse::from).toList(),
            result.page(),
            result.size(),
            result.totalElements(),
            result.totalPages()
        );
    }
}
