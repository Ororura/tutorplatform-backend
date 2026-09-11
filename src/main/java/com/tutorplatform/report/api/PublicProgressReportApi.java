package com.tutorplatform.report.api;

import com.tutorplatform.report.api.response.PublicProgressReportResponse;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;

public interface PublicProgressReportApi {

    @Operation(operationId = "getPublicProgressReport", summary = "Get a historical progress report by share token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Public historical progress report"),
        @ApiResponse(responseCode = "404", description = "Report share not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "410", description = "Report share expired or revoked", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    PublicProgressReportResponse getPublicProgressReport(String token);

    @Operation(operationId = "downloadPublicProgressReportPdf", summary = "Download a shared published progress report as PDF")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Public progress report PDF", content = @Content(
            mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "404", description = "Report share not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "410", description = "Report share expired or revoked", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "PDF generation failed", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<byte[]> downloadPublicProgressReportPdf(String token);
}
