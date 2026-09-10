package com.tutorplatform.assessment.api;

import com.tutorplatform.assessment.api.request.SaveTeacherAssessmentRequest;
import com.tutorplatform.assessment.api.response.TeacherAssessmentResponse;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface TeacherAssessmentApi {

    @Operation(operationId = "getTeacherAssessment", summary = "Get a lesson session assessment")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Assessment details"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Teacher role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Lesson session or assessment not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    TeacherAssessmentResponse getTeacherAssessment(
        AuthenticatedUser principal,
        UUID studentId,
        UUID sessionId
    );

    @Operation(operationId = "saveTeacherAssessment", summary = "Create or replace a lesson session assessment")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Assessment updated"),
        @ApiResponse(responseCode = "201", description = "Assessment created"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Teacher role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Lesson session not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Concurrent assessment conflict", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<TeacherAssessmentResponse> saveTeacherAssessment(
        AuthenticatedUser principal,
        UUID studentId,
        UUID sessionId,
        SaveTeacherAssessmentRequest request
    );
}
