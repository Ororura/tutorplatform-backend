package com.tutorplatform.homework.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.homework.api.request.CreateHomeworkRequest;
import com.tutorplatform.homework.api.request.UpdateHomeworkRequest;
import com.tutorplatform.homework.api.response.HomeworkDetailsResponse;
import com.tutorplatform.homework.api.response.HomeworkPageResponse;
import com.tutorplatform.homework.domain.HomeworkStatus;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface TeacherHomeworkApi {

    @Operation(operationId = "createHomework", summary = "Create homework for a student")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Homework created"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Teacher role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Student, program, or task not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<HomeworkDetailsResponse> createHomework(
            AuthenticatedUser principal,
            UUID studentId,
            CreateHomeworkRequest request
    );

    @Operation(operationId = "listHomeworks", summary = "List homework for a student")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Homework page"),
        @ApiResponse(responseCode = "400", description = "Invalid list parameters", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Teacher role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Student or program not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    HomeworkPageResponse listHomeworks(
            AuthenticatedUser principal,
            UUID studentId,
            UUID studentProgramId,
            HomeworkStatus status,
            @Parameter(description = "Zero-based page index", example = "0") int page,
            @Parameter(description = "Page size from 1 to 100", example = "20") int size,
            @Parameter(description = "Sort as field,direction", example = "assignedAt,desc") String sort
    );

    @Operation(operationId = "getHomework", summary = "Get homework details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Homework details"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Teacher role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Homework not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    HomeworkDetailsResponse getHomework(AuthenticatedUser principal, UUID studentId, UUID homeworkId);

    @Operation(operationId = "updateHomework", summary = "Update assigned homework")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Homework updated"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Teacher role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Homework not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Version or item conflict", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    HomeworkDetailsResponse updateHomework(
            AuthenticatedUser principal,
            UUID studentId,
            UUID homeworkId,
            UpdateHomeworkRequest request
    );

    @Operation(operationId = "cancelHomework", summary = "Cancel assigned homework")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Homework cancelled"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Teacher role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Homework not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    HomeworkDetailsResponse cancelHomework(AuthenticatedUser principal, UUID studentId, UUID homeworkId);
}
