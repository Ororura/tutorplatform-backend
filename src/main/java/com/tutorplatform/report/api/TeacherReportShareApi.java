package com.tutorplatform.report.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.report.api.request.CreateReportShareRequest;
import com.tutorplatform.report.api.response.ReportShareCreatedResponse;
import com.tutorplatform.report.api.response.ReportShareListResponse;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface TeacherReportShareApi {

    @Operation(operationId = "createReportShare", summary = "Create a historical progress-report share")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Report share created"),
        @ApiResponse(responseCode = "400", description = "Expiration is invalid", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Teacher role required or CSRF rejected", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Progress report not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Progress report is not published", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<ReportShareCreatedResponse> createReportShare(
        @Parameter(hidden = true) AuthenticatedUser principal,
        UUID reportId,
        CreateReportShareRequest request
    );

    @Operation(operationId = "listReportShares", summary = "List report-share metadata")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Report-share metadata"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Teacher role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Progress report not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ReportShareListResponse listReportShares(
        @Parameter(hidden = true) AuthenticatedUser principal,
        UUID reportId
    );

    @Operation(operationId = "revokeReportShare", summary = "Revoke a report share")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Report share revoked"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Teacher role required or CSRF rejected", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Progress report or share not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<Void> revokeReportShare(
        @Parameter(hidden = true) AuthenticatedUser principal,
        UUID reportId,
        UUID shareId
    );
}
