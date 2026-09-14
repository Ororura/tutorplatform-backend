package com.tutorplatform.program.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.List;
import java.util.UUID;

public interface TeacherStudentProgramApi {

    @Operation(
        operationId = "listTeacherStudentPrograms",
        summary = "List programs for a student owned by the current teacher"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Student program summaries"),
        @ApiResponse(responseCode = "401", description = "Authentication required",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Teacher role required",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Student not found",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    List<StudentProgramSummaryResponse> listPrograms(
        @Parameter(hidden = true) AuthenticatedUser principal,
        @Parameter(schema = @Schema(format = "uuid")) UUID studentId
    );

    @Operation(
        operationId = "getTeacherStudentProgram",
        summary = "Get a program structure for a student owned by the current teacher"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Student program details"),
        @ApiResponse(responseCode = "401", description = "Authentication required",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Teacher role required",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Student or student program not found",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    StudentProgramDetailsResponse getProgram(
        @Parameter(hidden = true) AuthenticatedUser principal,
        @Parameter(schema = @Schema(format = "uuid")) UUID studentId,
        @Parameter(schema = @Schema(format = "uuid")) UUID studentProgramId
    );
}
