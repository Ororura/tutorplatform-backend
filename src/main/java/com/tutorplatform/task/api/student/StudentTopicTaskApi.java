package com.tutorplatform.task.api.student;

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

public interface StudentTopicTaskApi {

    @Operation(
        operationId = "listStudentTopicTasks",
        summary = "List available practice tasks for an assigned program topic"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Available topic tasks"),
        @ApiResponse(responseCode = "401", description = "Authentication required",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Student role required",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Student program or topic not found",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    List<StudentTopicTaskResponse> listTasks(
        @Parameter(hidden = true) AuthenticatedUser principal,
        @Parameter(schema = @Schema(format = "uuid")) UUID studentProgramId,
        @Parameter(schema = @Schema(format = "uuid")) UUID topicId
    );
}
