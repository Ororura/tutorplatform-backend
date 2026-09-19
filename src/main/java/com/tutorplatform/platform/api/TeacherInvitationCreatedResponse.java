package com.tutorplatform.platform.api;

import com.tutorplatform.platform.application.invite.TeacherRegistrationInviteService;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record TeacherInvitationCreatedResponse(
    @Schema(format = "uuid")
    UUID id,

    @Schema(format = "email")
    String email,

    @Schema(format = "date-time")
    Instant expiresAt,

    @Schema(format = "uri")
    String invitationUrl,

    @Schema(format = "date-time")
    Instant createdAt
) {

    public static TeacherInvitationCreatedResponse from(
        TeacherRegistrationInviteService.CreatedInvitation invitation
    ) {
        return new TeacherInvitationCreatedResponse(
            invitation.id(),
            invitation.email(),
            invitation.expiresAt(),
            invitation.invitationUrl(),
            invitation.createdAt()
        );
    }
}
