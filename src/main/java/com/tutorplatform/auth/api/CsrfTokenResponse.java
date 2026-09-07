package com.tutorplatform.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;

public record CsrfTokenResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String token,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String headerName
) {
}
