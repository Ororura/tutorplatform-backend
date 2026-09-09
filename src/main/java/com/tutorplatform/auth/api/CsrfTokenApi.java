package com.tutorplatform.auth.api;

import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CsrfToken;


public interface CsrfTokenApi {
    @Operation(operationId = "getCsrfToken", summary = "Get CSRF token")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = CsrfTokenResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Unexpected server failure",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    CsrfTokenResponse getCsrfToken(@Parameter(hidden = true) CsrfToken csrfToken);
}
