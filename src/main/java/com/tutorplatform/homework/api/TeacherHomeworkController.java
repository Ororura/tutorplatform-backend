package com.tutorplatform.homework.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.homework.api.request.CreateHomeworkRequest;
import com.tutorplatform.homework.api.request.HomeworkItemRequest;
import com.tutorplatform.homework.api.request.UpdateHomeworkRequest;
import com.tutorplatform.homework.api.response.HomeworkDetailsResponse;
import com.tutorplatform.homework.api.response.HomeworkPageResponse;
import com.tutorplatform.homework.application.CreateHomeworkCommand;
import com.tutorplatform.homework.application.HomeworkItemInput;
import com.tutorplatform.homework.application.HomeworkService;
import com.tutorplatform.homework.application.UpdateHomeworkCommand;
import com.tutorplatform.homework.domain.HomeworkStatus;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher/students/{studentId}/homeworks")
public class TeacherHomeworkController implements TeacherHomeworkApi {

    private final HomeworkService homeworkService;

    public TeacherHomeworkController(HomeworkService homeworkService) {
        this.homeworkService = homeworkService;
    }

    @Override
    @PostMapping
    public ResponseEntity<HomeworkDetailsResponse> createHomework(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID studentId,
        @Valid @RequestBody CreateHomeworkRequest request
    ) {
        var created = homeworkService.createHomework(principal, new CreateHomeworkCommand(
            studentId, request.studentProgramId(), request.title(), request.description(),
            request.dueAt(), toItemInputs(request.items())
        ));
        return ResponseEntity.created(URI.create(
            "/api/v1/teacher/students/" + studentId + "/homeworks/" + created.id()
        )).body(HomeworkDetailsResponse.from(created));
    }

    @Override
    @GetMapping
    public HomeworkPageResponse listHomeworks(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID studentId,
        @RequestParam(required = false) UUID studentProgramId,
        @RequestParam(required = false) HomeworkStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "assignedAt,desc") String sort
    ) {
        return HomeworkPageResponse.from(homeworkService.listHomeworks(
            principal, studentId, studentProgramId, status, page, size, sort
        ));
    }

    @Override
    @GetMapping("/{homeworkId}")
    public HomeworkDetailsResponse getHomework(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID studentId,
        @PathVariable UUID homeworkId
    ) {
        return HomeworkDetailsResponse.from(homeworkService.getHomework(
            principal, studentId, homeworkId
        ));
    }

    @Override
    @PatchMapping("/{homeworkId}")
    public HomeworkDetailsResponse updateHomework(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID studentId,
        @PathVariable UUID homeworkId,
        @Valid @RequestBody UpdateHomeworkRequest request
    ) {
        return HomeworkDetailsResponse.from(homeworkService.updateHomework(
            principal,
            studentId,
            homeworkId,
            new UpdateHomeworkCommand(
                request.title(), request.description(), request.dueAt(), request.version(),
                toItemInputs(request.items())
            )
        ));
    }

    @Override
    @PostMapping("/{homeworkId}/cancel")
    public HomeworkDetailsResponse cancelHomework(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID studentId,
        @PathVariable UUID homeworkId
    ) {
        return HomeworkDetailsResponse.from(homeworkService.cancelHomework(
            principal, studentId, homeworkId
        ));
    }

    private List<HomeworkItemInput> toItemInputs(List<HomeworkItemRequest> items) {
        return items.stream()
            .map(item -> new HomeworkItemInput(item.taskId(), item.position(), item.required()))
            .toList();
    }
}
