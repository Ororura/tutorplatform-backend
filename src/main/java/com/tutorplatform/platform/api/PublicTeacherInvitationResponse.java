package com.tutorplatform.platform.api;

import com.tutorplatform.platform.application.invite.TeacherRegistrationInviteService;
import com.tutorplatform.platform.domain.TeacherRegistrationInviteStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record PublicTeacherInvitationResponse(

    @Schema(format = "email")
    String email,

    TeacherRegistrationInviteStatus status,

    @Schema(format = "date-time")
    Instant expiresAt

) {

    public static PublicTeacherInvitationResponse from(
        TeacherRegistrationInviteService.PublicInvitation invitation
    ) {
        return new PublicTeacherInvitationResponse(
            invitation.email(),
            invitation.status(),
            invitation.expiresAt()
        );
    }
}
