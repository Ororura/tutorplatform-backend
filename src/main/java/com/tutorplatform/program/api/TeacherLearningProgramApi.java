package com.tutorplatform.program.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

public interface TeacherLearningProgramApi {
    @Operation(
            operationId = "bulkUpdateTeacherLearningProgramTopicStatus",
            summary = "Atomically update topic statuses in an owned editable learning program")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Topic statuses updated"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request or duplicate topic IDs",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Teacher role and valid CSRF token required",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Learning program or topic not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Learning program cannot be edited or topic version is stale",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<Void> bulkUpdateTopicStatus(
            @Parameter(hidden = true) AuthenticatedUser principal,
            @Parameter(schema = @Schema(format = "uuid")) UUID programId,
            @Valid BulkUpdateLearningProgramTopicStatusRequest request);

    @Operation(
            operationId = "listTeacherLearningPrograms",
            summary = "List learning program templates owned by the current teacher")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Learning program templates"),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Teacher role required",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    List<LearningProgramSummaryResponse> listPrograms(
            @Parameter(hidden = true) AuthenticatedUser principal, LearningProgramStatus status);

    @Operation(
            operationId = "getTeacherLearningProgram",
            summary = "Get an owned learning program template")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Learning program template"),
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
                description = "Learning program not found",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    LearningProgramDetailsResponse getProgram(
            @Parameter(hidden = true) AuthenticatedUser principal,
            @Parameter(schema = @Schema(format = "uuid")) UUID programId);

    @Operation(
            operationId = "getTeacherLearningProgramBySlug",
            summary = "Get an owned learning program template by slug")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Learning program template"),
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
                description = "Learning program not found",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    LearningProgramDetailsResponse getProgramBySlug(
            @Parameter(hidden = true) AuthenticatedUser principal, String slug);

    @Operation(
            operationId = "createTeacherLearningProgram",
            summary = "Create a draft learning program template")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Learning program created"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Subject not found",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<LearningProgramSummaryResponse> createProgram(
            @Parameter(hidden = true) AuthenticatedUser principal,
            @Valid CreateLearningProgramRequest request);

    @Operation(
            operationId = "createTeacherLearningProgramModule",
            summary = "Create a module in an owned editable learning program")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Module created"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Learning program not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Learning program cannot be edited",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<LearningProgramModuleResponse> createModule(
            @Parameter(hidden = true) AuthenticatedUser principal,
            @Parameter(schema = @Schema(format = "uuid")) UUID programId,
            @Valid CreateLearningProgramModuleRequest request);

    @Operation(
            operationId = "reorderTeacherLearningProgramModules",
            summary = "Reorder every module in an owned editable learning program")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Modules reordered"),
        @ApiResponse(
                responseCode = "400",
                description = "Module order is invalid",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Learning program not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Learning program cannot be edited",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<Void> reorderModules(
            @Parameter(hidden = true) AuthenticatedUser principal,
            @Parameter(schema = @Schema(format = "uuid")) UUID programId,
            @Valid ReorderLearningProgramModulesRequest request);

    @Operation(
            operationId = "createTeacherLearningProgramTopic",
            summary = "Create a topic in a module of an owned editable learning program")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Topic created"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Learning program or module not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Learning program cannot be edited",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<LearningProgramTopicDetailsResponse> createTopic(
            @Parameter(hidden = true) AuthenticatedUser principal,
            @Parameter(schema = @Schema(format = "uuid")) UUID programId,
            @Parameter(schema = @Schema(format = "uuid")) UUID moduleId,
            @Valid CreateLearningProgramTopicRequest request);

    @Operation(
            operationId = "reorderTeacherLearningProgramTopics",
            summary = "Reorder every topic in a module of an owned editable learning program")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Topics reordered"),
        @ApiResponse(
                responseCode = "400",
                description = "Topic order is invalid",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Learning program or module not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Learning program cannot be edited",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<Void> reorderTopics(
            @Parameter(hidden = true) AuthenticatedUser principal,
            @Parameter(schema = @Schema(format = "uuid")) UUID programId,
            @Parameter(schema = @Schema(format = "uuid")) UUID moduleId,
            @Valid ReorderLearningProgramTopicsRequest request);

    @Operation(
            operationId = "updateTeacherLearningProgramModule",
            summary = "Update a module in an owned editable learning program")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Module updated"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Learning program or module not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Learning program cannot be edited",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    LearningProgramModuleResponse updateModule(
            @Parameter(hidden = true) AuthenticatedUser principal,
            @Parameter(schema = @Schema(format = "uuid")) UUID programId,
            @Parameter(schema = @Schema(format = "uuid")) UUID moduleId,
            @Valid UpdateLearningProgramModuleRequest request);

    @Operation(
            operationId = "updateTeacherLearningProgramTopic",
            summary = "Update a topic in an owned editable learning program")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Topic updated"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Learning program, module, or topic not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Learning program cannot be edited or version is stale",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    LearningProgramTopicDetailsResponse updateTopic(
            @Parameter(hidden = true) AuthenticatedUser principal,
            @Parameter(schema = @Schema(format = "uuid")) UUID programId,
            @Parameter(schema = @Schema(format = "uuid")) UUID moduleId,
            @Parameter(schema = @Schema(format = "uuid")) UUID topicId,
            @Valid UpdateLearningProgramTopicRequest request);

    @Operation(
            operationId = "deleteTeacherLearningProgramModule",
            summary = "Delete an empty module from an owned editable learning program")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Module deleted"),
        @ApiResponse(
                responseCode = "404",
                description = "Learning program or module not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Learning program cannot be edited or module is not empty",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<Void> deleteModule(
            @Parameter(hidden = true) AuthenticatedUser principal,
            @Parameter(schema = @Schema(format = "uuid")) UUID programId,
            @Parameter(schema = @Schema(format = "uuid")) UUID moduleId);

    @Operation(
            operationId = "updateTeacherLearningProgram",
            summary = "Update an owned unassigned learning program")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Learning program updated"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Learning program not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Program cannot be edited or version is stale",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    LearningProgramDetailsResponse updateProgram(
            @Parameter(hidden = true) AuthenticatedUser principal,
            @Parameter(schema = @Schema(format = "uuid")) UUID programId,
            @Valid UpdateLearningProgramRequest request);

    @Operation(
            operationId = "activateTeacherLearningProgram",
            summary = "Activate an owned draft learning program")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Learning program activated"),
        @ApiResponse(
                responseCode = "404",
                description = "Learning program not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Invalid status transition",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    LearningProgramSummaryResponse activateProgram(
            @Parameter(hidden = true) AuthenticatedUser principal,
            @Parameter(schema = @Schema(format = "uuid")) UUID programId);

    @Operation(
            operationId = "archiveTeacherLearningProgram",
            summary = "Archive an owned learning program")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Learning program archived"),
        @ApiResponse(
                responseCode = "404",
                description = "Learning program not found",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    LearningProgramSummaryResponse archiveProgram(
            @Parameter(hidden = true) AuthenticatedUser principal,
            @Parameter(schema = @Schema(format = "uuid")) UUID programId);
}
