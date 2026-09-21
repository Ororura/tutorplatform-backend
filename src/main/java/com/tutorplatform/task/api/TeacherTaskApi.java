package com.tutorplatform.task.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.shared.api.ApiError;
import com.tutorplatform.task.api.programming.ProgrammingTaskConfigResponse;
import com.tutorplatform.task.api.programming.UpdateProgrammingTaskConfigRequest;
import com.tutorplatform.task.api.testcase.ReplaceTaskTestCasesRequest;
import com.tutorplatform.task.api.testcase.TaskTestCasesResponse;
import com.tutorplatform.task.api.topic.AttachTaskToTopicRequest;
import com.tutorplatform.task.api.topic.TopicTaskDetailsResponse;
import com.tutorplatform.task.api.topic.TopicTaskResponse;
import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

public interface TeacherTaskApi {

    @Operation(operationId = "createTask", summary = "Create a TEXT or fully configured CODE task")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Task created"),
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
                description = "Subject not found",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<TaskResponse> createTask(AuthenticatedUser principal, CreateTaskRequest request);

    @Operation(operationId = "listTasks", summary = "List the current teacher's tasks")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task page"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid list parameters",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Teacher role required",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    TaskPageResponse listTasks(
            AuthenticatedUser principal,
            UUID subjectId,
            TaskStatus status,
            TaskDifficulty difficulty,
            @Parameter(description = "Zero-based page index", example = "0") int page,
            @Parameter(description = "Page size from 1 to 100", example = "20") int size,
            @Parameter(description = "Sort as field,direction", example = "createdAt,desc")
                    String sort);

    @Operation(
            operationId = "getTask",
            summary = "Get a task; CODE details include all teacher-visible tests")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task details"),
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
                description = "Task not found",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    TaskResponse getTask(AuthenticatedUser principal, UUID taskId);

    @Operation(
            operationId = "updateTask",
            summary = "Update common task fields and lifecycle status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task updated"),
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
                description = "Task not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Version conflict",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    TaskResponse updateTask(AuthenticatedUser principal, UUID taskId, UpdateTaskRequest request);

    @Operation(
            operationId = "updateProgrammingTaskConfig",
            summary = "Update CODE task configuration")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Configuration updated"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid configuration or task type",
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
                description = "Task not found",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ProgrammingTaskConfigResponse updateProgrammingTaskConfig(
            AuthenticatedUser principal, UUID taskId, UpdateProgrammingTaskConfigRequest request);

    @Operation(
            operationId = "replaceTaskTestCases",
            summary = "Atomically replace CODE task test cases")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Test cases replaced"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid test cases or task type",
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
                description = "Task not found",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    TaskTestCasesResponse replaceTaskTestCases(
            AuthenticatedUser principal, UUID taskId, ReplaceTaskTestCasesRequest request);

    @Operation(operationId = "listTopicTasks", summary = "List tasks attached to an owned topic")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Attached tasks"),
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
    List<TopicTaskDetailsResponse> listTopicTasks(AuthenticatedUser principal, UUID topicId);

    @Operation(operationId = "attachTaskToTopic", summary = "Attach a text task to a topic")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Task attached"),
        @ApiResponse(
                responseCode = "400",
                description = "Validation or subject mismatch",
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
                description = "Task or topic not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Attachment conflict",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<TopicTaskResponse> attachTaskToTopic(
            AuthenticatedUser principal,
            UUID topicId,
            UUID taskId,
            AttachTaskToTopicRequest request);
}
