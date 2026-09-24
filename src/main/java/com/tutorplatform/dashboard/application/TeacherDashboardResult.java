package com.tutorplatform.dashboard.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TeacherDashboardResult(
        long activeStudentsCount,
        long needsReviewSubmissionsCount,
        long overdueHomeworksCount,
        long completedLearningPeriodsWithoutPublishedReportCount,
        List<AttentionItem> attentionItems) {

    public record AttentionItem(
            TeacherDashboardAttentionType type,
            UUID studentId,
            String displayName,
            UUID resourceId,
            Instant eventAt,
            Navigation navigation) {}

    public record Navigation(
            UUID studentProgramId,
            UUID homeworkId,
            UUID homeworkItemId,
            UUID taskId,
            UUID submissionId,
            UUID learningPeriodId,
            UUID reportId) {}
}
