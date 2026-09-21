package com.tutorplatform.platform.api;

import com.tutorplatform.platform.application.invite.TeacherRegistrationInviteService;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/teacher-invitations")
public class AdminTeacherInvitationController {

    private final TeacherRegistrationInviteService invitationService;

    public AdminTeacherInvitationController(TeacherRegistrationInviteService invitationService) {
        this.invitationService = invitationService;
    }

    @Operation(
            operationId = "createTeacherInvitation",
            summary = "Create a teacher registration invitation")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Invitation created"),
        @ApiResponse(
                responseCode = "400",
                description = "Validation failed",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Admin role required",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Email already registered",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<TeacherInvitationCreatedResponse> createInvitation(
            @AuthenticationPrincipal(expression = "id()") UUID adminId,
            @Valid @RequestBody CreateTeacherInvitationRequest request) {
        var invitation = invitationService.createInvitation(adminId, request.email());

        return ResponseEntity.created(
                        URI.create("/api/v1/admin/teacher-invitations/" + invitation.id()))
                .cacheControl(CacheControl.noStore())
                .body(TeacherInvitationCreatedResponse.from(invitation));
    }

    @Operation(
            operationId = "listTeacherInvitations",
            summary = "List teacher registration invitations")
    @GetMapping
    public TeacherInvitationListResponse listInvitations(
            @AuthenticationPrincipal(expression = "id()") UUID adminId) {
        var invitations =
                invitationService.listInvitations(adminId).stream()
                        .map(TeacherInvitationSummaryResponse::from)
                        .toList();

        return new TeacherInvitationListResponse(invitations);
    }

    @Operation(
            operationId = "revokeTeacherInvitation",
            summary = "Revoke a teacher registration invitation")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Invitation revoked"),
        @ApiResponse(
                responseCode = "404",
                description = "Invitation not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Invitation is not active",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{invitationId}")
    public ResponseEntity<Void> revokeInvitation(
            @AuthenticationPrincipal(expression = "id()") UUID adminId,
            @PathVariable UUID invitationId) {
        invitationService.revokeInvitation(adminId, invitationId);

        return ResponseEntity.noContent().build();
    }
}
