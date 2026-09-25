package com.tutorplatform.content.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.api.response.ContentPackageImportResponse;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface TeacherContentPackageImportApi {
    @Operation(
            operationId = "importTutorContentPackage",
            summary = "Confirm a YAML content package import",
            description =
                    "Imports a previously previewed YAML file (maximum 1 MiB) into an owned editable program. Requires a teacher session and CSRF token. Repeating a successful confirmation with the same digest returns the stored result without creating content.",
            requestBody =
                    @RequestBody(
                            required = true,
                            content =
                                    @Content(
                                            mediaType = "multipart/form-data",
                                            schema =
                                                    @Schema(
                                                            implementation =
                                                                    ContentPackageImportUpload
                                                                            .class))))
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Content imported",
                content =
                        @Content(
                                schema =
                                        @Schema(
                                                implementation =
                                                        ContentPackageImportResponse.class))),
        @ApiResponse(
                responseCode = "200",
                description = "Idempotent replay of the stored import result",
                content =
                        @Content(
                                schema =
                                        @Schema(
                                                implementation =
                                                        ContentPackageImportResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request, YAML, or package content",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
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
                description = "Digest or confirmation conflict, or program cannot be edited",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "413",
                description = "File exceeds 1 MiB or configured multipart limit",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<ContentPackageImportResponse> importPackage(
            @Parameter(hidden = true) AuthenticatedUser principal,
            @Parameter(schema = @Schema(format = "uuid")) UUID programId,
            MultipartFile file,
            UUID confirmationId,
            String digest);

    record ContentPackageImportUpload(
            @Schema(
                            description = "Required YAML file, maximum 1 MiB",
                            type = "string",
                            format = "binary",
                            requiredMode = Schema.RequiredMode.REQUIRED)
                    MultipartFile file,
            @Schema(
                            description = "UUID of the import confirmation",
                            format = "uuid",
                            requiredMode = Schema.RequiredMode.REQUIRED)
                    UUID confirmationId,
            @Schema(
                            description = "Lowercase SHA-256 digest returned by Preview API",
                            pattern = "[0-9a-f]{64}",
                            requiredMode = Schema.RequiredMode.REQUIRED)
                    String digest) {}
}
