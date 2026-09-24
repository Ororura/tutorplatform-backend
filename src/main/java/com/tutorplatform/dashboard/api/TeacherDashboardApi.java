package com.tutorplatform.dashboard.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

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
}
