package com.tutorplatform.content.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.api.response.ContentPackagePreviewFailureResponse;
import com.tutorplatform.content.api.response.ContentPackagePreviewResponse;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface TeacherContentPackagePreviewApi {
    @Operation(
            operationId = "previewTutorContentPackage",
            summary = "Preview a YAML content package for an owned editable learning program",
            description =
                    "Checks a .yaml or .yml file (maximum 1 MiB) using the package parser and validator without importing content. Invalid YAML and content return HTTP 400 with code, path, and message for each error.",
            requestBody =
                    @RequestBody(
                            required = true,
                            content =
                                    @Content(
                                            mediaType = "multipart/form-data",
                                            schema =
                                                    @Schema(
                                                            implementation =
                                                                    ContentPackagePreviewUpload
                                                                            .class))))
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Preview formed",
                content =
                        @Content(
                                schema =
                                        @Schema(
                                                implementation =
                                                        ContentPackagePreviewResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid YAML, file, or package content",
                content =
                        @Content(
                                schema =
                                        @Schema(
                                                implementation =
                                                        ContentPackagePreviewFailureResponse
                                                                .class))),
        @ApiResponse(
                responseCode = "401",
                description = "Session authentication required",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Teacher role and CSRF token required",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Program absent or owned by another teacher",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Program cannot be edited",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "413",
                description = "File exceeds 1 MiB or configured multipart limit",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ContentPackagePreviewResponse preview(
            @Parameter(hidden = true) AuthenticatedUser principal,
            @Parameter(schema = @Schema(format = "uuid")) UUID programId,
            MultipartFile file);

    record ContentPackagePreviewUpload(
            @Schema(
                            description = "Required YAML file; .yaml or .yml; maximum 1 MiB",
                            type = "string",
                            format = "binary",
                            requiredMode = Schema.RequiredMode.REQUIRED)
                    MultipartFile file) {}
}
