package com.tutorplatform.report.api.response;

import com.tutorplatform.report.domain.ProgressReport;
import com.tutorplatform.report.domain.ProgressReportSnapshotV1;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(name = "PublicProgressReportResponse", description = "Public, read-only historical progress report")
public record PublicProgressReportResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant periodStartedAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant periodEndedAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int learningMinutes,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PublicProgressReportSnapshot snapshot,
    @Schema(types = {"string", "null"}) String teacherSummary,
    @Schema(types = {"string", "null"}) String nextPeriodPlan,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant publishedAt
) {
    public static PublicProgressReportResponse from(ProgressReport report) {
        return new PublicProgressReportResponse(
            report.periodStartedAt(),
            report.periodEndedAt(),
            report.learningMinutes(),
            PublicProgressReportSnapshot.from(report.snapshot()),
            report.teacherSummary(),
            report.nextPeriodPlan(),
            report.publishedAt()
        );
    }

    public record PublicProgressReportSnapshot(
        Metrics metrics,
        Assessment assessment,
        Topics topics,
        List<Skill> skills
    ) {
        private static PublicProgressReportSnapshot from(ProgressReportSnapshotV1 snapshot) {
            ProgressReportSnapshotV1.Metrics metrics = snapshot.metrics();
            ProgressReportSnapshotV1.Assessment assessment = snapshot.assessment();
            return new PublicProgressReportSnapshot(
                new Metrics(
                    metrics.learningMinutes(), metrics.sessionsCount(), metrics.attendanceRate(),
                    metrics.homeworkAssigned(), metrics.homeworkCompleted(),
                    metrics.practiceAssigned(), metrics.practiceCompleted()
                ),
                new Assessment(
                    assessment.understandingAverage(), assessment.independenceAverage(),
                    assessment.practiceAverage(), assessment.homeworkAverage()
                ),
                new Topics(
                    snapshot.topics().completed().stream().map(topic -> new Topic(topic.title())).toList(),
                    snapshot.topics().inProgress().stream().map(topic -> new Topic(topic.title())).toList()
                ),
                snapshot.skills().stream()
                    .map(skill -> new Skill(skill.name(), skill.progress()))
                    .toList()
            );
        }
    }

    public record Metrics(
        int learningMinutes,
        long sessionsCount,
        double attendanceRate,
        long homeworkAssigned,
        long homeworkCompleted,
        long practiceAssigned,
        long practiceCompleted
    ) {
    }

    public record Assessment(
        @Schema(types = {"number", "null"}) BigDecimal understandingAverage,
        @Schema(types = {"number", "null"}) BigDecimal independenceAverage,
        @Schema(types = {"number", "null"}) BigDecimal practiceAverage,
        @Schema(types = {"number", "null"}) BigDecimal homeworkAverage
    ) {
    }

    public record Topics(List<Topic> completed, List<Topic> inProgress) {
    }

    public record Topic(String title) {
    }

    public record Skill(String name, BigDecimal progress) {
    }
}
