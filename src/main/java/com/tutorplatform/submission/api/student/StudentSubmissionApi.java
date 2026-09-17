package com.tutorplatform.submission.api.student;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.shared.api.ApiError;
import com.tutorplatform.submission.api.SubmitCodeAnswerRequest;
import com.tutorplatform.submission.api.SubmitTextAnswerRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface StudentSubmissionApi {

    @Operation(operationId = "submitTextAnswer", summary = "Submit a TEXT answer for a homework task")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "TEXT submission created for teacher review"),
        @ApiResponse(responseCode = "400", description = "Validation or submission context error", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Student role or CSRF token required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Task or homework item not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Homework does not accept submissions", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<StudentSubmissionResponse> submitTextAnswer(
        @Parameter(hidden = true) AuthenticatedUser principal,
        @Parameter(schema = @Schema(format = "uuid")) UUID taskId,
        SubmitTextAnswerRequest request
    );

    @Operation(operationId = "submitCodeAnswer", summary = "Submit and execute a CODE solution for a homework task")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "CODE submission created and executed"),
        @ApiResponse(responseCode = "400", description = "Validation or submission context error", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Student role or CSRF token required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Task or homework item not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Homework does not accept submissions", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<StudentSubmissionResponse> submitCodeAnswer(
        @Parameter(hidden = true) AuthenticatedUser principal,
        @Parameter(schema = @Schema(format = "uuid")) UUID taskId,
        SubmitCodeAnswerRequest request
    );

    @Operation(operationId = "listStudentTaskSubmissions", summary = "List the current student's task submissions")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Student submission page"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination or context", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Student role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Task or homework item not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    StudentSubmissionPageResponse listStudentTaskSubmissions(
        @Parameter(hidden = true) AuthenticatedUser principal,
        @Parameter(schema = @Schema(format = "uuid")) UUID taskId,
        @Parameter(description = "Optional homework item context", schema = @Schema(format = "uuid")) UUID homeworkItemId,
        @Parameter(description = "Zero-based page index", example = "0") int page,
        @Parameter(description = "Page size from 1 to 100", example = "20") int size
    );

    @Operation(operationId = "getStudentSubmission", summary = "Get one submission owned by the current student")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Student submission"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Student role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Submission not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    StudentSubmissionResponse getStudentSubmission(
        @Parameter(hidden = true) AuthenticatedUser principal,
        @Parameter(schema = @Schema(format = "uuid")) UUID submissionId
    );
}
