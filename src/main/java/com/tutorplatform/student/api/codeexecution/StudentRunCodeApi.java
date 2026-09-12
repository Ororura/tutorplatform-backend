package com.tutorplatform.student.api.codeexecution;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.shared.api.ApiError;
import com.tutorplatform.student.api.requrest.RunCodeRequest;
import com.tutorplatform.student.api.response.RunCodeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.UUID;

public interface StudentRunCodeApi {

    @Operation(
        operationId = "runCode",
        summary = "Run code for an assigned CODE task without creating a submission"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Student-safe execution result"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Student role and CSRF token required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Task or execution context not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Task cannot be executed", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    RunCodeResponse runCode(
        @Parameter(hidden = true) AuthenticatedUser principal,
        @Parameter(schema = @Schema(format = "uuid")) UUID taskId,
        RunCodeRequest request
    );
}
