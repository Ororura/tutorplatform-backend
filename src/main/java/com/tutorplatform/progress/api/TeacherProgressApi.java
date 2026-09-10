package com.tutorplatform.progress.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.progress.api.response.CurrentProgressResponse;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.UUID;

public interface TeacherProgressApi {

    @Operation(operationId = "getTeacherStudentProgress", summary = "Get current progress for an owned student")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Current progress"),
        @ApiResponse(responseCode = "400", description = "Missing or invalid student program id", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Teacher role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Student or student program not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    CurrentProgressResponse getTeacherStudentProgress(
        @Parameter(hidden = true) AuthenticatedUser principal,
        @Parameter(schema = @Schema(format = "uuid")) UUID studentId,
        @Parameter(description = "Student program belonging to the requested student", required = true, schema = @Schema(format = "uuid")) UUID studentProgramId
    );
}
