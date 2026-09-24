package com.tutorplatform.dashboard.application;

import java.util.List;
import java.util.UUID;

public interface TeacherDashboardQuery {

    Summary getSummary(UUID teacherUserId);

    List<TeacherDashboardResult.AttentionItem> findAttentionItems(UUID teacherUserId, int limit);

    record Summary(
            long activeStudentsCount,
            long needsReviewSubmissionsCount,
            long overdueHomeworksCount,
            long completedLearningPeriodsWithoutPublishedReportCount) {}
}
