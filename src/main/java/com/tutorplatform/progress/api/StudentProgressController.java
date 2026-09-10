package com.tutorplatform.progress.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.progress.api.response.CurrentProgressResponse;
import com.tutorplatform.progress.application.ProgressAccessService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student/progress")
public class StudentProgressController implements StudentProgressApi {

    private final ProgressAccessService progressAccessService;

    public StudentProgressController(ProgressAccessService progressAccessService) {
        this.progressAccessService = progressAccessService;
    }

    @Override
    @GetMapping
    public CurrentProgressResponse getCurrentStudentProgress(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @RequestParam UUID studentProgramId
    ) {
        return CurrentProgressResponse.from(
            progressAccessService.getForStudent(principal, studentProgramId)
        );
    }
}
