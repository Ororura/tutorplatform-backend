package com.tutorplatform.submission.api.student;

import com.tutorplatform.submission.application.SubmissionPageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record StudentSubmissionPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<StudentSubmissionResponse> items,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int page,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "100")
                int size,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long totalElements,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int totalPages) {
    public static StudentSubmissionPageResponse from(SubmissionPageResult result) {
        return new StudentSubmissionPageResponse(
                result.items().stream().map(StudentSubmissionResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }
}
