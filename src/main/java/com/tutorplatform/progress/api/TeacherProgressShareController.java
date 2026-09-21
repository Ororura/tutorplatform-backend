package com.tutorplatform.progress.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.progress.api.request.CreateProgressShareRequest;
import com.tutorplatform.progress.api.response.ProgressShareCreatedResponse;
import com.tutorplatform.progress.api.response.ProgressShareListResponse;
import com.tutorplatform.progress.application.ProgressShareService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher/students/{studentId}/progress/shares")
public class TeacherProgressShareController implements TeacherProgressShareApi {

    private final ProgressShareService progressShareService;

    public TeacherProgressShareController(ProgressShareService progressShareService) {
        this.progressShareService = progressShareService;
    }

    @Override
    @PostMapping
    public ResponseEntity<ProgressShareCreatedResponse> createProgressShare(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID studentId,
            @Valid @RequestBody CreateProgressShareRequest request) {
        ProgressShareCreatedResponse response =
                progressShareService.create(principal, studentId, request);
        return ResponseEntity.created(
                        URI.create(
                                "/api/v1/teacher/students/"
                                        + studentId
                                        + "/progress/shares/"
                                        + response.id()))
                .body(response);
    }

    @Override
    @GetMapping
    public ProgressShareListResponse listProgressShares(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID studentId,
            @RequestParam(required = false) UUID studentProgramId) {
        return progressShareService.list(principal, studentId, studentProgramId);
    }

    @Override
    @DeleteMapping("/{shareId}")
    public ResponseEntity<Void> revokeProgressShare(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID studentId,
            @PathVariable UUID shareId) {
        progressShareService.revoke(principal, studentId, shareId);
        return ResponseEntity.noContent().build();
    }
}
