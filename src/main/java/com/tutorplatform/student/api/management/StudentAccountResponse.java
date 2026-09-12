package com.tutorplatform.student.api.management;

import com.tutorplatform.student.domain.StudentAccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record StudentAccountResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) StudentAccountStatus status,
    @Schema(nullable = true) String email
) {
}
