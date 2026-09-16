package com.tutorplatform.submission.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.submission.api.request.SubmitCodeAnswerRequest;
import com.tutorplatform.submission.api.request.SubmitTextAnswerRequest;
import com.tutorplatform.submission.api.response.StudentSubmissionPageResponse;
import com.tutorplatform.submission.api.response.StudentSubmissionResponse;
import com.tutorplatform.submission.application.SubmissionService;
import com.tutorplatform.submission.application.CodeSubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student")
public class StudentSubmissionController implements StudentSubmissionApi {

    private final SubmissionService submissionService;
    private final CodeSubmissionService codeSubmissionService;

    public StudentSubmissionController(
        SubmissionService submissionService,
        CodeSubmissionService codeSubmissionService
    ) {
        this.submissionService = submissionService;
        this.codeSubmissionService = codeSubmissionService;
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
        return created(response);
    }

    @Override
    @PostMapping("/tasks/{taskId}/code-submissions")
    public ResponseEntity<StudentSubmissionResponse> submitCodeAnswer(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID taskId,
        @Valid @RequestBody SubmitCodeAnswerRequest request
    ) {
        StudentSubmissionResponse response = StudentSubmissionResponse.from(
            codeSubmissionService.submit(
                principal, taskId, request.homeworkItemId(), request.sourceCode()
            )
        );
        return created(response);
    }

    private ResponseEntity<StudentSubmissionResponse> created(StudentSubmissionResponse response) {
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
