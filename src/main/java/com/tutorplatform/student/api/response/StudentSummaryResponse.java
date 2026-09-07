package com.tutorplatform.student.api.response;

import com.tutorplatform.student.domain.StudentAccountStatus;
import com.tutorplatform.student.domain.StudentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record StudentSummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String firstName,
        @Schema(nullable = true) String lastName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) StudentStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) StudentAccountStatus accountStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant createdAt
) {
}
