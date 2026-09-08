package com.tutorplatform.homework.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.homework.api.response.StudentHomeworkDetailsResponse;
import com.tutorplatform.homework.api.response.StudentHomeworkPageResponse;
import com.tutorplatform.homework.domain.HomeworkStatus;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.UUID;

public interface StudentHomeworkApi {

    @Operation(operationId = "listStudentHomeworks", summary = "List homework assigned to the current student")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Student homework page"),
        @ApiResponse(responseCode = "400", description = "Invalid list parameters", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Student role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Student or student program not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    StudentHomeworkPageResponse listStudentHomeworks(
            @Parameter(hidden = true) AuthenticatedUser principal,
            @Parameter(description = "Optional student program owned by the current student") UUID studentProgramId,
            HomeworkStatus status,
            @Parameter(description = "Zero-based page index", example = "0") int page,
            @Parameter(description = "Page size from 1 to 100", example = "20") int size,
            @Parameter(description = "Sort as field,direction", example = "assignedAt,desc") String sort
    );

    @Operation(operationId = "getStudentHomework", summary = "Get homework assigned to the current student")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Student homework details"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Student role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Homework not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    StudentHomeworkDetailsResponse getStudentHomework(
            @Parameter(hidden = true) AuthenticatedUser principal,
            @Parameter(schema = @Schema(format = "uuid")) UUID homeworkId
    );
}
