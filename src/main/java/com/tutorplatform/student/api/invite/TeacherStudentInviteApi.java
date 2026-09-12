package com.tutorplatform.student.api.invite;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface TeacherStudentInviteApi {

    @Operation(
        operationId = "createStudentInvite",
        summary = "Create an invitation for a student"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Invitation created"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Teacher role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Student not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Student or email conflict", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<StudentInviteCreatedResponse> createInvite(
        AuthenticatedUser principal,
        UUID studentId,
        CreateStudentInviteRequest request
    );

    @Operation(
        operationId = "listStudentInvites",
        summary = "List invitation metadata for a student"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invitation list"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Teacher role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Student not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    StudentInviteListResponse listInvites(AuthenticatedUser principal, UUID studentId);

    @Operation(
        operationId = "revokeStudentInvite",
        summary = "Revoke a student invitation"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Invitation revoked"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Teacher role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Student or invitation not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
            responseCode = "409",
            description = "Invitation already accepted",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    ResponseEntity<Void> revokeInvite(
        AuthenticatedUser principal,
        UUID studentId,
        UUID inviteId
    );
}
