package com.tutorplatform.dashboard.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherDashboardService {

    static final int ATTENTION_ITEM_LIMIT = 20;

    private final TeacherDashboardQuery dashboardQuery;

    public TeacherDashboardService(TeacherDashboardQuery dashboardQuery) {
        this.dashboardQuery = dashboardQuery;
    }

    @Transactional(readOnly = true)
    public TeacherDashboardResult getDashboard(AuthenticatedUser principal) {
        TeacherDashboardQuery.Summary summary = dashboardQuery.getSummary(principal.id());
        return new TeacherDashboardResult(
                summary.activeStudentsCount(),
                summary.needsReviewSubmissionsCount(),
                summary.overdueHomeworksCount(),
                summary.completedLearningPeriodsWithoutPublishedReportCount(),
                dashboardQuery.findAttentionItems(principal.id(), ATTENTION_ITEM_LIMIT));
    }
}
