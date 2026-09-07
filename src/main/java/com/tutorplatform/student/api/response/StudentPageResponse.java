package com.tutorplatform.student.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record StudentPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<StudentSummaryResponse> items,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int page,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "100") int size,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long totalElements,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int totalPages
) {
}
