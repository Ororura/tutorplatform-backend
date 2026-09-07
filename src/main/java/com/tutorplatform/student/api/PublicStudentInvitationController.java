package com.tutorplatform.student.api;

import com.tutorplatform.auth.api.CurrentUserResponse;
import com.tutorplatform.shared.api.ApiError;
import com.tutorplatform.student.api.requrest.AcceptStudentInviteRequest;
import com.tutorplatform.student.api.response.PublicStudentInviteResponse;
import com.tutorplatform.student.application.PublicStudentInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/student-invitations")
public class PublicStudentInvitationController {

    private final PublicStudentInvitationService invitationService;

    public PublicStudentInvitationController(PublicStudentInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @Operation(operationId = "getPublicStudentInvitation", summary = "Get public student invitation metadata")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitation metadata"),
            @ApiResponse(responseCode = "404", description = "Invitation not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "410", description = "Invitation is expired, revoked, or accepted", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping(value = "/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
    public PublicStudentInviteResponse getPublicStudentInvitation(@PathVariable String token) {
        return invitationService.getInvitation(token);
    }

    @Operation(operationId = "acceptStudentInvitation", summary = "Accept a student invitation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitation accepted and student authenticated"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid CSRF token", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Invitation not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Student or email conflict", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "410", description = "Invitation is expired, revoked, or accepted", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping(value = "/{token}/accept", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CurrentUserResponse acceptStudentInvitation(
            @PathVariable String token,
            @Valid @RequestBody AcceptStudentInviteRequest request,
            @Parameter(hidden = true) HttpServletRequest servletRequest,
            @Parameter(hidden = true) HttpServletResponse servletResponse
    ) {
        return invitationService.acceptInvitation(token, request, servletRequest, servletResponse);
    }
}
