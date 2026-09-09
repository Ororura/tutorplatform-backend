package com.tutorplatform.submission.api.response;

import com.tutorplatform.submission.application.SubmissionPageResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record TeacherSubmissionPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<TeacherSubmissionResponse> items,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int page,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "100") int size,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long totalElements,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int totalPages
) {
    public static TeacherSubmissionPageResponse from(SubmissionPageResult result) {
        return new TeacherSubmissionPageResponse(
                result.items().stream().map(TeacherSubmissionResponse::from).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages()
        );
    }
}
