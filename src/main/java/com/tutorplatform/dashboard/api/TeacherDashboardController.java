package com.tutorplatform.dashboard.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.dashboard.application.TeacherDashboardAttentionType;
import com.tutorplatform.dashboard.application.TeacherDashboardService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher/dashboard")
public class TeacherDashboardController implements TeacherDashboardApi {

    private final TeacherDashboardService dashboardService;

    public TeacherDashboardController(TeacherDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Override
    @GetMapping
    public TeacherDashboardResponse getTeacherDashboard(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return TeacherDashboardResponse.from(dashboardService.getDashboard(principal));
    }

    @Override
    @GetMapping("/attention")
    public TeacherDashboardAttentionPageResponse listAttentionItems(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) TeacherDashboardAttentionType type,
            @RequestParam(required = false) UUID studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return TeacherDashboardAttentionPageResponse.from(
                dashboardService.getAttentionItems(principal, type, studentId, page, size));
    }
}
