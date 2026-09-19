package com.tutorplatform.platform.api;

import com.tutorplatform.platform.application.invite.TeacherRegistrationInviteService;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/teacher-invitations")
public class PublicTeacherInvitationController {

    private final TeacherRegistrationInviteService invitationService;

    public PublicTeacherInvitationController(
        TeacherRegistrationInviteService invitationService
    ) {
        this.invitationService = invitationService;
    }

    @Operation(
        operationId = "getPublicTeacherInvitation",
        summary = "Get teacher registration invitation"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Invitation details"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Invitation not found",
            content = @Content(
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @GetMapping("/{token}")
    public ResponseEntity<PublicTeacherInvitationResponse> getInvitation(
        @PathVariable String token
    ) {
        var invitation = invitationService.getPublicInvitation(token);

        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(PublicTeacherInvitationResponse.from(invitation));
    }
}
