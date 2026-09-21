package com.tutorplatform.progress.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.progress.api.response.CurrentProgressResponse;
import com.tutorplatform.progress.application.ProgressAccessService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher/students/{studentId}/progress")
public class TeacherProgressController implements TeacherProgressApi {

    private final ProgressAccessService progressAccessService;

    public TeacherProgressController(ProgressAccessService progressAccessService) {
        this.progressAccessService = progressAccessService;
    }

    @Override
    @GetMapping
    public CurrentProgressResponse getTeacherStudentProgress(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID studentId,
            @RequestParam UUID studentProgramId) {
        return CurrentProgressResponse.from(
                progressAccessService.getForTeacher(principal, studentId, studentProgramId));
    }
}
