package com.tutorplatform.shared.api;

import io.swagger.v3.oas.annotations.media.Schema;

public record ApiErrorDetail(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String field,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String message
) {
}
