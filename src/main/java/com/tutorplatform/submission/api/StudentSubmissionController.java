package com.tutorplatform.submission.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.submission.api.request.SubmitTextAnswerRequest;
import com.tutorplatform.submission.api.response.StudentSubmissionPageResponse;
import com.tutorplatform.submission.api.response.StudentSubmissionResponse;
import com.tutorplatform.submission.application.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student")
public class StudentSubmissionController implements StudentSubmissionApi {

    private final SubmissionService submissionService;

    public StudentSubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @Override
    @PostMapping("/tasks/{taskId}/submissions")
    public ResponseEntity<StudentSubmissionResponse> submitTextAnswer(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID taskId,
            @Valid @RequestBody SubmitTextAnswerRequest request
    ) {
        StudentSubmissionResponse response = StudentSubmissionResponse.from(
                submissionService.submitTextAnswer(
                        principal, taskId, request.homeworkItemId(), request.textAnswer()
                )
        );
        return ResponseEntity.created(URI.create(
                "/api/v1/student/submissions/" + response.id()
        )).body(response);
    }

    @Override
    @GetMapping("/tasks/{taskId}/submissions")
    public StudentSubmissionPageResponse listStudentTaskSubmissions(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID taskId,
            @RequestParam(required = false) UUID homeworkItemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return StudentSubmissionPageResponse.from(submissionService.listStudentTaskSubmissions(
                principal, taskId, homeworkItemId, page, size
        ));
    }

    @Override
    @GetMapping("/submissions/{submissionId}")
    public StudentSubmissionResponse getStudentSubmission(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID submissionId
    ) {
        return StudentSubmissionResponse.from(
                submissionService.getStudentSubmission(principal, submissionId)
        );
    }
}
