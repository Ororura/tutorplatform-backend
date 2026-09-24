package com.tutorplatform.dashboard.api;

import com.tutorplatform.dashboard.application.TeacherDashboardAttentionType;
import com.tutorplatform.dashboard.application.TeacherDashboardResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TeacherDashboardResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
                long activeStudentsCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
                long needsReviewSubmissionsCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
                long overdueHomeworksCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
                long completedLearningPeriodsWithoutPublishedReportCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<AttentionItem> attentionItems) {

    static TeacherDashboardResponse from(TeacherDashboardResult result) {
        return new TeacherDashboardResponse(
                result.activeStudentsCount(),
                result.needsReviewSubmissionsCount(),
                result.overdueHomeworksCount(),
                result.completedLearningPeriodsWithoutPublishedReportCount(),
                result.attentionItems().stream().map(AttentionItem::from).toList());
    }

    public record AttentionItem(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TeacherDashboardAttentionType type,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID studentId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String displayName,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID resourceId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
                    Instant eventAt,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Navigation navigation) {

        private static AttentionItem from(TeacherDashboardResult.AttentionItem item) {
            return new AttentionItem(
                    item.type(),
                    item.studentId(),
                    item.displayName(),
                    item.resourceId(),
                    item.eventAt(),
                    Navigation.from(item.navigation()));
        }
    }

    public record Navigation(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid")
                    UUID studentProgramId,
            @Schema(nullable = true, format = "uuid") UUID homeworkId,
            @Schema(nullable = true, format = "uuid") UUID homeworkItemId,
            @Schema(nullable = true, format = "uuid") UUID taskId,
            @Schema(nullable = true, format = "uuid") UUID submissionId,
            @Schema(nullable = true, format = "uuid") UUID learningPeriodId,
            @Schema(nullable = true, format = "uuid") UUID reportId) {

        private static Navigation from(TeacherDashboardResult.Navigation navigation) {
            return new Navigation(
                    navigation.studentProgramId(),
                    navigation.homeworkId(),
                    navigation.homeworkItemId(),
                    navigation.taskId(),
                    navigation.submissionId(),
                    navigation.learningPeriodId(),
                    navigation.reportId());
        }
    }
}
