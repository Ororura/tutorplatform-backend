package com.tutorplatform.submission.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.shared.api.ApiError;
import com.tutorplatform.submission.api.request.ReviewTextSubmissionRequest;
import com.tutorplatform.submission.api.response.TeacherSubmissionPageResponse;
import com.tutorplatform.submission.api.response.TeacherSubmissionResponse;
import com.tutorplatform.submission.domain.SubmissionStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.UUID;

public interface TeacherSubmissionApi {

    @Operation(operationId = "listTeacherStudentSubmissions", summary = "List a teacher's student's submissions")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Submission page"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination, filter, or sort", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Teacher role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Student not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    TeacherSubmissionPageResponse listTeacherStudentSubmissions(
        @Parameter(hidden = true) AuthenticatedUser principal,
        @Parameter(schema = @Schema(format = "uuid")) UUID studentId,
        SubmissionStatus status,
        @Parameter(description = "Zero-based page index", example = "0") int page,
        @Parameter(description = "Page size from 1 to 100", example = "20") int size,
        @Parameter(description = "Sort as submittedAt|attemptNo|status,asc|desc", example = "submittedAt,desc") String sort
    );

    @Operation(operationId = "reviewTextSubmission", summary = "Review a text submission")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Submission reviewed"),
        @ApiResponse(responseCode = "400", description = "Invalid review status", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Teacher role or CSRF token required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Student or submission not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Submission is not reviewable", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    TeacherSubmissionResponse reviewTextSubmission(
        @Parameter(hidden = true) AuthenticatedUser principal,
        @Parameter(schema = @Schema(format = "uuid")) UUID studentId,
        @Parameter(schema = @Schema(format = "uuid")) UUID submissionId,
        ReviewTextSubmissionRequest request
    );
}
