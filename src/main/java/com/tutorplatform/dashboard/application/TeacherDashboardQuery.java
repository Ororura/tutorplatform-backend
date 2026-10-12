package com.tutorplatform.dashboard.application;

import java.util.List;
import java.util.UUID;

public interface TeacherDashboardQuery {

    Summary getSummary(UUID teacherUserId);

    List<TeacherDashboardResult.AttentionItem> findAttentionItems(UUID teacherUserId, int limit);

    AttentionPage findAttentionItems(
            UUID teacherUserId,
            TeacherDashboardAttentionType type,
            UUID studentId,
            int page,
            int size);

    record Summary(
            long activeStudentsCount,
            long needsReviewSubmissionsCount,
            long overdueHomeworksCount,
            long completedLearningPeriodsWithoutPublishedReportCount) {}

    record AttentionPage(List<TeacherDashboardResult.AttentionItem> items, long totalElements) {}
}
