package com.tutorplatform.student.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record PublicStudentInviteResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) StudentName student,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TeacherName teacher,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "email") String email,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant expiresAt
) {
    public record StudentName(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String firstName,
            @Schema(nullable = true) String lastName
    ) {
    }

    public record TeacherName(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String displayName
    ) {
    }
}
