package com.tutorplatform.subject.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.shared.api.ApiError;
import com.tutorplatform.subject.domain.SubjectStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.List;

public interface TeacherSubjectApi {
    @Operation(operationId = "listTeacherSubjects", summary = "List subjects available to the current teacher")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Available subjects"),
        @ApiResponse(responseCode = "401", description = "Authentication required",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Teacher role required",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    List<SubjectSummaryResponse> listSubjects(
        @Parameter(hidden = true) AuthenticatedUser principal,
        SubjectStatus status
    );
}
