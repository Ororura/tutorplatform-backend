package com.tutorplatform.task.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.task.api.topic.AttachTaskToTopicRequest;
import com.tutorplatform.task.api.programming.UpdateProgrammingTaskConfigRequest;
import com.tutorplatform.task.api.testcase.ReplaceTaskTestCasesRequest;
import com.tutorplatform.task.api.topic.TopicTaskResponse;
import com.tutorplatform.task.api.programming.ProgrammingTaskConfigResponse;
import com.tutorplatform.task.api.testcase.TaskTestCasesResponse;
import com.tutorplatform.task.application.AttachTaskToTopicCommand;
import com.tutorplatform.task.application.CreateTaskCommand;
import com.tutorplatform.task.application.TaskService;
import com.tutorplatform.task.application.UpdateTaskCommand;
import com.tutorplatform.task.application.ProgrammingTaskConfigInput;
import com.tutorplatform.task.application.TaskTestCaseInput;
import com.tutorplatform.task.application.UpdateProgrammingTaskConfigCommand;
import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskStatus;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher")
public class TeacherTaskController implements TeacherTaskApi {

    private final TaskService taskService;

    public TeacherTaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    @PostMapping("/tasks")
    public ResponseEntity<TaskResponse> createTask(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @Valid @RequestBody CreateTaskRequest request
    ) {
        var created = taskService.createTask(principal, new CreateTaskCommand(
            request.subjectId(), request.title(), request.descriptionMarkdown(), request.difficulty(),
            request.taskType(),
            request.programmingConfig() == null ? null : new ProgrammingTaskConfigInput(
                request.programmingConfig().language(), request.programmingConfig().starterCode(),
                request.programmingConfig().executionEnabled(), request.programmingConfig().timeLimitMs(),
                request.programmingConfig().memoryLimitMb()
            ),
            request.testCases() == null ? null : request.testCases().stream().map(item -> new TaskTestCaseInput(
                item.id(), item.inputText(), item.expectedOutput(), item.hidden(),
                item.comparisonMode(), item.position()
            )).toList()
        ));
        return ResponseEntity.created(URI.create("/api/v1/teacher/tasks/" + created.id()))
            .body(TaskResponse.from(created));
    }

    @Override
    @PutMapping("/tasks/{taskId}/programming-config")
    public ProgrammingTaskConfigResponse updateProgrammingTaskConfig(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID taskId,
        @Valid @RequestBody UpdateProgrammingTaskConfigRequest request
    ) {
        return ProgrammingTaskConfigResponse.from(taskService.updateProgrammingTaskConfig(
            principal, taskId, new UpdateProgrammingTaskConfigCommand(
                request.starterCode(), request.executionEnabled(), request.timeLimitMs(), request.memoryLimitMb()
            )
        ));
    }

    @Override
    @PutMapping("/tasks/{taskId}/test-cases")
    public TaskTestCasesResponse replaceTaskTestCases(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID taskId,
        @Valid @RequestBody ReplaceTaskTestCasesRequest request
    ) {
        return TaskTestCasesResponse.from(taskService.replaceTaskTestCases(
            principal, taskId, request.items().stream().map(item -> new TaskTestCaseInput(
                item.id(), item.inputText(), item.expectedOutput(), item.hidden(),
                item.comparisonMode(), item.position()
            )).toList()
        ));
    }

    @Override
    @GetMapping("/tasks")
    public TaskPageResponse listTasks(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @RequestParam(required = false) UUID subjectId,
        @RequestParam(required = false) TaskStatus status,
        @RequestParam(required = false) TaskDifficulty difficulty,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return TaskPageResponse.from(taskService.listTasks(
            principal, subjectId, status, difficulty, page, size, sort
        ));
    }

    @Override
    @GetMapping("/tasks/{taskId}")
    public TaskResponse getTask(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID taskId
    ) {
        return TaskResponse.from(taskService.getTask(principal, taskId));
    }

    @Override
    @PatchMapping("/tasks/{taskId}")
    public TaskResponse updateTask(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID taskId,
        @Valid @RequestBody UpdateTaskRequest request
    ) {
        return TaskResponse.from(taskService.updateTask(principal, taskId, new UpdateTaskCommand(
            request.title(), request.descriptionMarkdown(), request.difficulty(),
            request.status(), request.version()
        )));
    }

    @Override
    @PostMapping("/topics/{topicId}/tasks/{taskId}")
    public ResponseEntity<TopicTaskResponse> attachTaskToTopic(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID topicId,
        @PathVariable UUID taskId,
        @Valid @RequestBody AttachTaskToTopicRequest request
    ) {
        var attached = taskService.attachTaskToTopic(
            principal,
            topicId,
            taskId,
            new AttachTaskToTopicCommand(request.position(), request.required())
        );
        return ResponseEntity.created(URI.create(
            "/api/v1/teacher/topics/" + topicId + "/tasks/" + taskId
        )).body(TopicTaskResponse.from(attached));
    }
}
