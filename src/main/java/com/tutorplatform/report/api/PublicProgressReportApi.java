package com.tutorplatform.report.api;

import com.tutorplatform.report.api.response.PublicProgressReportResponse;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

public interface PublicProgressReportApi {

    @Operation(operationId = "getPublicProgressReport", summary = "Get a historical progress report by share token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Public historical progress report"),
        @ApiResponse(responseCode = "404", description = "Report share not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "410", description = "Report share expired or revoked", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    PublicProgressReportResponse getPublicProgressReport(String token);
}
