package com.tutorplatform.homework.api.response;

import com.tutorplatform.homework.application.StudentHomeworkPageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record StudentHomeworkPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
                List<StudentHomeworkSummaryResponse> items,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int page,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "100")
                int size,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long totalElements,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int totalPages) {
    public static StudentHomeworkPageResponse from(StudentHomeworkPageResult result) {
        return new StudentHomeworkPageResponse(
                result.items().stream().map(StudentHomeworkSummaryResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }
}
