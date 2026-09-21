package com.tutorplatform.student.api.invation;

import com.tutorplatform.auth.api.CurrentUserResponse;
import com.tutorplatform.shared.api.ApiError;
import com.tutorplatform.student.api.invite.AcceptStudentInviteRequest;
import com.tutorplatform.student.api.invite.PublicStudentInviteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface PublicStudentInvitationApi {

    @Operation(
            operationId = "getPublicStudentInvitation",
            summary = "Get public student invitation metadata")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invitation metadata"),
        @ApiResponse(
                responseCode = "404",
                description = "Invitation not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "410",
                description = "Invitation is expired, revoked, or accepted",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    PublicStudentInviteResponse getPublicStudentInvitation(String token);

    @Operation(operationId = "acceptStudentInvitation", summary = "Accept a student invitation")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Invitation accepted and student authenticated"),
        @ApiResponse(
                responseCode = "400",
                description = "Validation failed",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Missing or invalid CSRF token",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Invitation not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Student or email conflict",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "410",
                description = "Invitation is expired, revoked, or accepted",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    CurrentUserResponse acceptStudentInvitation(
            String token,
            AcceptStudentInviteRequest request,
            @Parameter(hidden = true) HttpServletRequest servletRequest,
            @Parameter(hidden = true) HttpServletResponse servletResponse);
}
