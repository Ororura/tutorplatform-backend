package com.tutorplatform.platform.api;

import com.tutorplatform.platform.application.invite.TeacherRegistrationInviteService;
import com.tutorplatform.platform.domain.TeacherRegistrationInviteStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record TeacherInvitationSummaryResponse(
    @Schema(format = "uuid")
    UUID id,

    @Schema(format = "email")
    String email,

    TeacherRegistrationInviteStatus status,

    @Schema(format = "date-time")
    Instant expiresAt,

    @Schema(format = "date-time")
    Instant createdAt
) {

    public static TeacherInvitationSummaryResponse from(
        TeacherRegistrationInviteService.InvitationSummary invitation
    ) {
        return new TeacherInvitationSummaryResponse(
            invitation.id(),
            invitation.email(),
            invitation.status(),
            invitation.expiresAt(),
            invitation.createdAt()
        );
    }
}
