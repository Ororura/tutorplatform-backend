package com.tutorplatform.subject.api;

import com.tutorplatform.subject.domain.SubjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record SubjectSummaryResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
    @Schema(nullable = true) String code,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
    @Schema(nullable = true) String description,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SubjectStatus status
) {
}
