package com.tutorplatform.program.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public interface TeacherLearningProgramApi {
    @Operation(operationId = "listTeacherLearningPrograms", summary = "List learning program templates owned by the current teacher")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Learning program templates"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Teacher role required", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    List<LearningProgramSummaryResponse> listPrograms(
        @Parameter(hidden = true) AuthenticatedUser principal,
        LearningProgramStatus status
    );

    @Operation(operationId = "getTeacherLearningProgram", summary = "Get an owned learning program template")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Learning program template"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Teacher role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Learning program not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    LearningProgramDetailsResponse getProgram(
        @Parameter(hidden = true) AuthenticatedUser principal,
        @Parameter(schema = @Schema(format = "uuid")) UUID programId
    );

    @Operation(operationId = "createTeacherLearningProgram", summary = "Create a draft learning program template")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Learning program created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Subject not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<LearningProgramSummaryResponse> createProgram(
        @Parameter(hidden = true) AuthenticatedUser principal,
        @Valid CreateLearningProgramRequest request
    );

    @Operation(operationId = "activateTeacherLearningProgram", summary = "Activate an owned draft learning program")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Learning program activated"),
        @ApiResponse(responseCode = "404", description = "Learning program not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Invalid status transition", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    LearningProgramSummaryResponse activateProgram(
        @Parameter(hidden = true) AuthenticatedUser principal,
        @Parameter(schema = @Schema(format = "uuid")) UUID programId
    );
}
