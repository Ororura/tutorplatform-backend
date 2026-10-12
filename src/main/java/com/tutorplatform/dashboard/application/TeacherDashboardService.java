package com.tutorplatform.dashboard.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.student.application.management.StudentNotFoundException;
import com.tutorplatform.student.application.ownership.StudentOwnershipQuery;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherDashboardService {

    static final int ATTENTION_ITEM_LIMIT = 20;

    private final TeacherDashboardQuery dashboardQuery;
    private final StudentOwnershipQuery studentOwnershipQuery;

    public TeacherDashboardService(
            TeacherDashboardQuery dashboardQuery, StudentOwnershipQuery studentOwnershipQuery) {
        this.dashboardQuery = dashboardQuery;
        this.studentOwnershipQuery = studentOwnershipQuery;
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

    @Transactional(readOnly = true)
    public TeacherDashboardAttentionPage getAttentionItems(
            AuthenticatedUser principal,
            TeacherDashboardAttentionType type,
            UUID studentId,
            int page,
            int size) {
        validatePage(page, size);
        if (studentId != null) {
            UUID teacherId =
                    studentOwnershipQuery.findTeacherIdByUserId(principal.id()).orElseThrow();
            if (!studentOwnershipQuery.isActivePrimaryOwner(teacherId, studentId)) {
                throw new StudentNotFoundException();
            }
        }
        TeacherDashboardQuery.AttentionPage result =
                dashboardQuery.findAttentionItems(principal.id(), type, studentId, page, size);
        int totalPages = (int) Math.ceil((double) result.totalElements() / size);
        return new TeacherDashboardAttentionPage(
                result.items(), page, size, result.totalElements(), totalPages);
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            String field = page < 0 ? "page" : "size";
            String message =
                    page < 0 ? "must be greater than or equal to 0" : "must be between 1 and 100";
            throw new InvalidTeacherDashboardAttentionParameterException(field, message);
        }
    }
}
