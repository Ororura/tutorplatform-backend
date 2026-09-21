package com.tutorplatform.report.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Schema(name = "ProgressReportSnapshotV1")
public record ProgressReportSnapshotV1(
        Metrics metrics, Assessment assessment, Topics topics, List<Skill> skills) {
    public ProgressReportSnapshotV1 {
        Objects.requireNonNull(metrics, "metrics");
        Objects.requireNonNull(assessment, "assessment");
        Objects.requireNonNull(topics, "topics");
        skills = List.copyOf(skills);
    }

    public record Metrics(
            int learningMinutes,
            long sessionsCount,
            double attendanceRate,
            long homeworkAssigned,
            long homeworkCompleted,
            long practiceAssigned,
            long practiceCompleted) {
        public Metrics {
            if (learningMinutes < 0
                    || sessionsCount < 0
                    || homeworkAssigned < 0
                    || homeworkCompleted < 0
                    || practiceAssigned < 0
                    || practiceCompleted < 0) {
                throw new IllegalArgumentException("progress metrics must not be negative");
            }
            if (!Double.isFinite(attendanceRate) || attendanceRate < 0.0 || attendanceRate > 1.0) {
                throw new IllegalArgumentException("attendanceRate must be in the 0..1 range");
            }
        }
    }

    public record Assessment(
            BigDecimal understandingAverage,
            BigDecimal independenceAverage,
            BigDecimal practiceAverage,
            BigDecimal homeworkAverage) {}

    public record Topics(List<Topic> completed, List<Topic> inProgress) {
        public Topics {
            completed = List.copyOf(completed);
            inProgress = List.copyOf(inProgress);
        }
    }

    public record Topic(UUID id, String title) {
        public Topic {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(title, "title");
        }
    }

    /** Present in schema v1; calculation is deferred by CurrentProgress in the MVP. */
    public record Skill(UUID skillId, String name, BigDecimal progress) {
        public Skill {
            Objects.requireNonNull(skillId, "skillId");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(progress, "progress");
        }
    }
}
