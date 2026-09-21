package com.tutorplatform.report.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.report.api.request.CreateProgressReportRequest;
import com.tutorplatform.report.api.request.PublishProgressReportRequest;
import com.tutorplatform.report.api.request.UpdateProgressReportRequest;
import com.tutorplatform.report.api.response.ProgressReportDetailsResponse;
import com.tutorplatform.report.api.response.ProgressReportPageResponse;
import com.tutorplatform.report.domain.ProgressReportStatus;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

public interface TeacherProgressReportApi {

    @Operation(operationId = "createProgressReport", summary = "Create a progress report draft")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Draft created"),
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
                description = "Student program or learning period not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Report already exists or period is invalid",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<ProgressReportDetailsResponse> createProgressReport(
            @Parameter(hidden = true) AuthenticatedUser principal,
            CreateProgressReportRequest request);

    @Operation(
            operationId = "listProgressReports",
            summary = "List progress reports accessible to the current teacher")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Progress report page"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid pagination, filter, or sort",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Teacher role required",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ProgressReportPageResponse listProgressReports(
            @Parameter(hidden = true) AuthenticatedUser principal,
            UUID studentProgramId,
            ProgressReportStatus status,
            @Parameter(description = "Zero-based page index", example = "0") int page,
            @Parameter(description = "Page size from 1 to 100", example = "20") int size,
            @Parameter(description = "Allow-listed field,direction", example = "createdAt,desc")
                    String sort);

    @Operation(
            operationId = "getProgressReport",
            summary = "Get a persisted progress report snapshot")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Progress report details"),
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
                description = "Progress report not found",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ProgressReportDetailsResponse getProgressReport(
            @Parameter(hidden = true) AuthenticatedUser principal, UUID reportId);

    @Operation(
            operationId = "downloadProgressReportPdf",
            summary = "Download a published progress report as PDF")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Progress report PDF",
                content =
                        @Content(
                                mediaType = "application/pdf",
                                schema = @Schema(type = "string", format = "binary"))),
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
                description = "Progress report not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Only published reports can be exported",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "500",
                description = "PDF generation failed",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<byte[]> downloadProgressReportPdf(
            @Parameter(hidden = true) AuthenticatedUser principal, UUID reportId);

    @Operation(operationId = "updateProgressReport", summary = "Edit draft report text")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Draft updated"),
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
                description = "Progress report not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Report is not editable or version is stale",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ProgressReportDetailsResponse updateProgressReport(
            @Parameter(hidden = true) AuthenticatedUser principal,
            UUID reportId,
            UpdateProgressReportRequest request);

    @Operation(operationId = "publishProgressReport", summary = "Publish a draft report")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Report published"),
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
                description = "Progress report not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Report is not publishable or version is stale",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ProgressReportDetailsResponse publishProgressReport(
            @Parameter(hidden = true) AuthenticatedUser principal,
            UUID reportId,
            PublishProgressReportRequest request);
}
