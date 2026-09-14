package com.tutorplatform.program.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record ProgramSubjectResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
    @Schema(nullable = true) String code,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name
) {
}
