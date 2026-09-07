package com.tutorplatform.content.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.api.request.CreateLessonMaterialRequest;
import com.tutorplatform.content.api.request.UpdateLessonMaterialRequest;
import com.tutorplatform.content.api.response.LessonMaterialResponse;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public interface TeacherLessonMaterialApi {

    @Operation(operationId = "createLessonMaterial", summary = "Create a lesson material")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Lesson material created"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Teacher role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Topic not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Position conflict", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<LessonMaterialResponse> createLessonMaterial(
        AuthenticatedUser principal,
        @Parameter(description = "Topic identifier", schema = @Schema(format = "uuid")) UUID topicId,
        CreateLessonMaterialRequest request
    );

    @Operation(operationId = "listLessonMaterials", summary = "List lesson materials for a topic")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lesson materials ordered by position"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Teacher role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Topic not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    List<LessonMaterialResponse> listLessonMaterials(
        AuthenticatedUser principal,
        @Parameter(description = "Topic identifier", schema = @Schema(format = "uuid")) UUID topicId
    );

    @Operation(operationId = "getLessonMaterial", summary = "Get a lesson material")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lesson material details"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Teacher role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Lesson material not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    LessonMaterialResponse getLessonMaterial(
        AuthenticatedUser principal,
        @Parameter(description = "Topic identifier", schema = @Schema(format = "uuid")) UUID topicId,
        @Parameter(description = "Lesson material identifier", schema = @Schema(format = "uuid")) UUID materialId
    );

    @Operation(operationId = "updateLessonMaterial", summary = "Update a lesson material")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lesson material updated"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Teacher role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Lesson material not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Position or version conflict", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    LessonMaterialResponse updateLessonMaterial(
        AuthenticatedUser principal,
        @Parameter(description = "Topic identifier", schema = @Schema(format = "uuid")) UUID topicId,
        @Parameter(description = "Lesson material identifier", schema = @Schema(format = "uuid")) UUID materialId,
        UpdateLessonMaterialRequest request
    );
}
