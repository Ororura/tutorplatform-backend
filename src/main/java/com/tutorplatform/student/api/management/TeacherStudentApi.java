package com.tutorplatform.student.api.management;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.shared.api.ApiError;
import com.tutorplatform.student.domain.StudentAccountStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface TeacherStudentApi {

    @Operation(
        operationId = "listTeacherStudents",
        summary = "List students owned by the current teacher"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Student page"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid list parameters",
            content = @Content(
                schema = @Schema(implementation = ApiError.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required",
            content = @Content(
                schema = @Schema(implementation = ApiError.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Teacher role required",
            content = @Content(
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    StudentPageResponse listStudents(
        AuthenticatedUser principal,
        int page,
        int size,
        String query,
        StudentAccountStatus accountStatus,
        String sort
    );

    @Operation(
        operationId = "createStudent",
        summary = "Create a student owned by the current teacher"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Student created"),
        @ApiResponse(
            responseCode = "400",
            description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Teacher role required",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    ResponseEntity<StudentSummaryResponse> createStudent(
        AuthenticatedUser principal,
        CreateStudentRequest request
    );

    @Operation(
        operationId = "getStudent",
        summary = "Get a student owned by the current teacher"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Student details"),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Teacher role required",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Student not found",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    StudentDetailsResponse getStudent(AuthenticatedUser principal, UUID studentId);

    @Operation(
        operationId = "updateStudent",
        summary = "Update a student owned by the current teacher"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Student updated"),
        @ApiResponse(
            responseCode = "400",
            description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Teacher role required",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Student not found",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    UpdateStudentResponse updateStudent(
        AuthenticatedUser principal,
        UUID studentId,
        UpdateStudentRequest request
    );
}
