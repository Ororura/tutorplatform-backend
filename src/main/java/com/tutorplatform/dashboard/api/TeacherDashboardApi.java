package com.tutorplatform.dashboard.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.dashboard.application.TeacherDashboardAttentionType;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;

public interface TeacherDashboardApi {

    @Operation(
            operationId = "getTeacherDashboard",
            summary = "Get the current teacher's workspace summary")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Teacher dashboard"),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Teacher role required",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    TeacherDashboardResponse getTeacherDashboard(
            @Parameter(hidden = true) AuthenticatedUser principal);

    @Operation(
            operationId = "listTeacherDashboardAttention",
            summary = "List teacher attention items")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Attention item page"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid pagination",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
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
                description = "Student not found",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    TeacherDashboardAttentionPageResponse listAttentionItems(
            @Parameter(hidden = true) AuthenticatedUser principal,
            TeacherDashboardAttentionType type,
            UUID studentId,
            int page,
            int size);
}
