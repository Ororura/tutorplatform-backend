package com.tutorplatform.progress.api;

import com.tutorplatform.progress.api.response.PublicCurrentProgressResponse;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

public interface PublicProgressApi {

    @Operation(operationId = "getPublicCurrentProgress", summary = "Get live current progress by share token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Parent-safe live current progress"),
        @ApiResponse(responseCode = "404", description = "Progress share not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "410", description = "Progress share expired or revoked", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    PublicCurrentProgressResponse getPublicCurrentProgress(String token);
}
