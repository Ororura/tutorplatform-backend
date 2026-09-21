package com.tutorplatform.program.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
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

public interface StudentProgramApi {

    @Operation(
            operationId = "listStudentPrograms",
            summary = "List programs assigned to the current student")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Student program summaries"),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Student role required",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    List<StudentProgramSummaryResponse> listPrograms(
            @Parameter(hidden = true) AuthenticatedUser principal);

    @Operation(
            operationId = "getStudentProgram",
            summary = "Get an assigned program structure for the current student")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Student program details"),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Student role required",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Student program not found",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    StudentProgramDetailsResponse getProgram(
            @Parameter(hidden = true) AuthenticatedUser principal,
            @Parameter(schema = @Schema(format = "uuid")) UUID studentProgramId);

    @Operation(
            operationId = "getStudentProgramTopic",
            summary = "Get a topic from a program assigned to the current student")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Topic with ordered lesson materials"),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Student role required",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Student program or topic not found",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    StudentProgramTopicResponse getTopic(
            @Parameter(hidden = true) AuthenticatedUser principal,
            @Parameter(schema = @Schema(format = "uuid")) UUID studentProgramId,
            @Parameter(schema = @Schema(format = "uuid")) UUID topicId);

    @Operation(
            operationId = "downloadStudentProgramMaterial",
            summary = "Download a material from a program assigned to the current student")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Material content"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Student role required"),
        @ApiResponse(
                responseCode = "404",
                description = "Student program, topic, or material not found")
    })
    ResponseEntity<byte[]> downloadMaterial(
            @Parameter(hidden = true) AuthenticatedUser principal,
            @Parameter(schema = @Schema(format = "uuid")) UUID studentProgramId,
            @Parameter(schema = @Schema(format = "uuid")) UUID topicId,
            @Parameter(schema = @Schema(format = "uuid")) UUID materialId);
}
