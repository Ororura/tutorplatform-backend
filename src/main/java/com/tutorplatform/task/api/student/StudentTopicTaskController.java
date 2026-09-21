package com.tutorplatform.task.api.student;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.task.application.StudentTopicTaskService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student/programs/{studentProgramId}/topics/{topicId}/tasks")
public class StudentTopicTaskController implements StudentTopicTaskApi {

    private final StudentTopicTaskService taskService;

    public StudentTopicTaskController(StudentTopicTaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    @GetMapping
    public List<StudentTopicTaskResponse> listTasks(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID studentProgramId,
        @PathVariable UUID topicId
    ) {
        return taskService.listTasks(principal, studentProgramId, topicId).stream()
            .map(StudentTopicTaskResponse::from)
            .toList();
    }
}
