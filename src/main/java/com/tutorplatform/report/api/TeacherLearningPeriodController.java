package com.tutorplatform.report.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.report.api.response.LearningPeriodResponse;
import com.tutorplatform.report.application.LearningPeriodQueryService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher/students/{studentId}/programs/{studentProgramId}/learning-periods")
public class TeacherLearningPeriodController implements TeacherLearningPeriodApi {

    private final LearningPeriodQueryService queryService;

    public TeacherLearningPeriodController(LearningPeriodQueryService queryService) {
        this.queryService = queryService;
    }

    @Override
    @GetMapping
    public List<LearningPeriodResponse> listLearningPeriods(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID studentId,
            @PathVariable UUID studentProgramId) {
        return queryService.list(principal, studentId, studentProgramId).stream()
                .map(LearningPeriodResponse::from)
                .toList();
    }
}
