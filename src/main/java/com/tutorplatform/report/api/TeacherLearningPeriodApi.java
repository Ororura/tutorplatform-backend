package com.tutorplatform.report.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.report.api.response.LearningPeriodResponse;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import java.util.UUID;

public interface TeacherLearningPeriodApi {

    @Operation(
            operationId = "listTeacherStudentLearningPeriods",
            summary = "List persisted learning periods for an owned student program")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Learning periods in sequence order"),
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
                description = "Student or student program not found",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    List<LearningPeriodResponse> listLearningPeriods(
            @Parameter(hidden = true) AuthenticatedUser principal,
            @Parameter(schema = @Schema(format = "uuid")) UUID studentId,
            @Parameter(schema = @Schema(format = "uuid")) UUID studentProgramId);
}
