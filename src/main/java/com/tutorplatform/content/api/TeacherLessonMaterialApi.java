package com.tutorplatform.content.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.api.request.CreateLessonMaterialRequest;
import com.tutorplatform.content.api.request.ReorderLessonMaterialsRequest;
import com.tutorplatform.content.api.request.UpdateLessonMaterialRequest;
import com.tutorplatform.content.api.response.LessonMaterialResponse;
import com.tutorplatform.content.domain.LessonMaterialType;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface TeacherLessonMaterialApi {

    @Operation(
            operationId = "uploadLessonMaterial",
            summary = "Upload a FILE or IMAGE lesson material")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "File and material created"),
        @ApiResponse(responseCode = "400", description = "Invalid file or metadata"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Teacher role and CSRF required"),
        @ApiResponse(responseCode = "404", description = "Topic not found"),
        @ApiResponse(responseCode = "409", description = "Position conflict"),
        @ApiResponse(responseCode = "413", description = "File too large")
    })
    ResponseEntity<LessonMaterialResponse> uploadLessonMaterial(
            AuthenticatedUser principal,
            UUID topicId,
            @Parameter(schema = @Schema(allowableValues = {"FILE", "IMAGE"}))
                    LessonMaterialType materialType,
            String title,
            int position,
            MultipartFile file);

    @Operation(
            operationId = "downloadLessonMaterial",
            summary = "Download an owned lesson material attachment")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Attachment content"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Teacher role required"),
        @ApiResponse(responseCode = "404", description = "Lesson material not found")
    })
    ResponseEntity<byte[]> downloadLessonMaterial(
            AuthenticatedUser principal, UUID topicId, UUID materialId);

    @Operation(operationId = "createLessonMaterial", summary = "Create a lesson material")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Lesson material created"),
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
                description = "Topic not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Position conflict",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<LessonMaterialResponse> createLessonMaterial(
            AuthenticatedUser principal,
            @Parameter(description = "Topic identifier", schema = @Schema(format = "uuid"))
                    UUID topicId,
            CreateLessonMaterialRequest request);

    @Operation(
            operationId = "reorderLessonMaterials",
            summary = "Reorder every lesson material in an owned topic")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Lesson materials reordered"),
        @ApiResponse(
                responseCode = "400",
                description = "Lesson material order is invalid",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Teacher role and CSRF required",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Topic not found",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<Void> reorderLessonMaterials(
            AuthenticatedUser principal,
            @Parameter(description = "Topic identifier", schema = @Schema(format = "uuid"))
                    UUID topicId,
            ReorderLessonMaterialsRequest request);

    @Operation(operationId = "listLessonMaterials", summary = "List lesson materials for a topic")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lesson materials ordered by position"),
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
                description = "Topic not found",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    List<LessonMaterialResponse> listLessonMaterials(
            AuthenticatedUser principal,
            @Parameter(description = "Topic identifier", schema = @Schema(format = "uuid"))
                    UUID topicId);

    @Operation(operationId = "getLessonMaterial", summary = "Get a lesson material")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lesson material details"),
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
                description = "Lesson material not found",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    LessonMaterialResponse getLessonMaterial(
            AuthenticatedUser principal,
            @Parameter(description = "Topic identifier", schema = @Schema(format = "uuid"))
                    UUID topicId,
            @Parameter(
                            description = "Lesson material identifier",
                            schema = @Schema(format = "uuid"))
                    UUID materialId);

    @Operation(operationId = "updateLessonMaterial", summary = "Update a lesson material")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lesson material updated"),
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
                description = "Lesson material not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Position or version conflict",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    LessonMaterialResponse updateLessonMaterial(
            AuthenticatedUser principal,
            @Parameter(description = "Topic identifier", schema = @Schema(format = "uuid"))
                    UUID topicId,
            @Parameter(
                            description = "Lesson material identifier",
                            schema = @Schema(format = "uuid"))
                    UUID materialId,
            UpdateLessonMaterialRequest request);
}
