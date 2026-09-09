package com.tutorplatform.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

public record CurrentUserResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "teacher@example.com") String email,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Егор") String displayName,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<UserRole> roles
) {
}
