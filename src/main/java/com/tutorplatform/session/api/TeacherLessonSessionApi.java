package com.tutorplatform.session.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.session.api.request.CreateLessonSessionRequest;
import com.tutorplatform.session.api.request.UpdateLessonSessionRequest;
import com.tutorplatform.session.api.response.LessonSessionDetailsResponse;
import com.tutorplatform.session.api.response.LessonSessionPageResponse;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

public interface TeacherLessonSessionApi {

    @Operation(operationId = "createLessonSession", summary = "Create a lesson session")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Lesson session created"),
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
                description = "Teacher role required",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Student or program not found",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<LessonSessionDetailsResponse> createLessonSession(
            AuthenticatedUser principal, UUID studentId, CreateLessonSessionRequest request);

    @Operation(operationId = "listLessonSessions", summary = "List lesson sessions for a student")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lesson session page"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid list parameters",
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
    LessonSessionPageResponse listLessonSessions(
            AuthenticatedUser principal,
            UUID studentId,
            @Parameter(description = "Zero-based page index", example = "0") int page,
            @Parameter(description = "Page size from 1 to 100", example = "20") int size,
            @Parameter(description = "Sort as field,direction", example = "startedAt,desc")
                    String sort);

    @Operation(operationId = "getLessonSession", summary = "Get a lesson session")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lesson session details"),
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
                description = "Lesson session not found",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    LessonSessionDetailsResponse getLessonSession(
            AuthenticatedUser principal, UUID studentId, UUID sessionId);

    @Operation(operationId = "updateLessonSession", summary = "Update a lesson session")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lesson session updated"),
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
                description = "Teacher role required",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Lesson session not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Version conflict",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    LessonSessionDetailsResponse updateLessonSession(
            AuthenticatedUser principal,
            UUID studentId,
            UUID sessionId,
            UpdateLessonSessionRequest request);
}
