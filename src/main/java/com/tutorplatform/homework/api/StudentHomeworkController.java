package com.tutorplatform.homework.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.homework.api.response.StudentHomeworkDetailsResponse;
import com.tutorplatform.homework.api.response.StudentHomeworkPageResponse;
import com.tutorplatform.homework.application.StudentHomeworkService;
import com.tutorplatform.homework.domain.HomeworkStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student/homeworks")
public class StudentHomeworkController implements StudentHomeworkApi {

    private final StudentHomeworkService studentHomeworkService;

    public StudentHomeworkController(StudentHomeworkService studentHomeworkService) {
        this.studentHomeworkService = studentHomeworkService;
    }

    @Override
    @GetMapping
    public StudentHomeworkPageResponse listStudentHomeworks(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) UUID studentProgramId,
            @RequestParam(required = false) HomeworkStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "assignedAt,desc") String sort
    ) {
        return StudentHomeworkPageResponse.from(studentHomeworkService.listHomeworks(
                principal, studentProgramId, status, page, size, sort
        ));
    }

    @Override
    @GetMapping("/{homeworkId}")
    public StudentHomeworkDetailsResponse getStudentHomework(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID homeworkId
    ) {
        return StudentHomeworkDetailsResponse.from(
                studentHomeworkService.getHomework(principal, homeworkId)
        );
    }
}
