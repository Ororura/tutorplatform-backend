package com.tutorplatform.subject.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.subject.application.TeacherSubjectService;
import com.tutorplatform.subject.domain.SubjectStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher/subjects")
public class TeacherSubjectController implements TeacherSubjectApi {
    private final TeacherSubjectService service;

    public TeacherSubjectController(TeacherSubjectService service) {
        this.service = service;
    }

    @Override
    @GetMapping
    public List<SubjectSummaryResponse> listSubjects(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @RequestParam(defaultValue = "ACTIVE") SubjectStatus status
    ) {
        return service.list(principal, status);
    }
}
