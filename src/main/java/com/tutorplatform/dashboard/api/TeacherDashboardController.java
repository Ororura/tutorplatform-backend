package com.tutorplatform.dashboard.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.dashboard.application.TeacherDashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
