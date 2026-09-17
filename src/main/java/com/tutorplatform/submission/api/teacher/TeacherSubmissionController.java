package com.tutorplatform.submission.api.teacher;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.submission.api.ReviewTextSubmissionRequest;
import com.tutorplatform.submission.application.SubmissionService;
import com.tutorplatform.submission.domain.SubmissionStatus;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher/students/{studentId}/submissions")
public class TeacherSubmissionController implements TeacherSubmissionApi {

    private final SubmissionService submissionService;

    public TeacherSubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @Override
    @GetMapping
    public TeacherSubmissionPageResponse listTeacherStudentSubmissions(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID studentId,
        @RequestParam(required = false) SubmissionStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "submittedAt,desc") String sort
    ) {
        return TeacherSubmissionPageResponse.from(submissionService.listTeacherStudentSubmissions(
            principal, studentId, status, page, size, sort
        ));
    }

    @Override
    @PatchMapping("/{submissionId}/review")
    public TeacherSubmissionResponse reviewTextSubmission(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID studentId,
        @PathVariable UUID submissionId,
        @Valid @RequestBody ReviewTextSubmissionRequest request
    ) {
        return TeacherSubmissionResponse.from(submissionService.reviewTextSubmission(
            principal, studentId, submissionId, request.status()
        ));
    }
}
