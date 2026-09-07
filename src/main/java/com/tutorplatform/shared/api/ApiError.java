package com.tutorplatform.shared.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record ApiError(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String message,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant timestamp,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String traceId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ApiErrorDetail> details
) {
    public static ApiError of(String code, String message, String traceId) {
        return new ApiError(code, message, Instant.now(), traceId, List.of());
    }
}
