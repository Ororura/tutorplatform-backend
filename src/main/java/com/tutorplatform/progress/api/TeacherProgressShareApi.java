package com.tutorplatform.progress.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.progress.api.request.CreateProgressShareRequest;
import com.tutorplatform.progress.api.response.ProgressShareCreatedResponse;
import com.tutorplatform.progress.api.response.ProgressShareListResponse;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

public interface TeacherProgressShareApi {

    @Operation(
            operationId = "createProgressShare",
            summary = "Create a live current-progress share")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Progress share created"),
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
                description = "Teacher role required or CSRF rejected",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Student or student program not found",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<ProgressShareCreatedResponse> createProgressShare(
            @Parameter(hidden = true) AuthenticatedUser principal,
            UUID studentId,
            CreateProgressShareRequest request);

    @Operation(operationId = "listProgressShares", summary = "List progress share metadata")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Progress share metadata"),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Teacher role required",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Student or student program not found",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ProgressShareListResponse listProgressShares(
            @Parameter(hidden = true) AuthenticatedUser principal,
            UUID studentId,
            @Parameter(
                            description = "Optional student-program filter",
                            schema = @Schema(format = "uuid"))
                    UUID studentProgramId);

    @Operation(operationId = "revokeProgressShare", summary = "Revoke a progress share")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Progress share revoked"),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Teacher role required or CSRF rejected",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Student or progress share not found",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<Void> revokeProgressShare(
            @Parameter(hidden = true) AuthenticatedUser principal, UUID studentId, UUID shareId);
}
