package com.tutorplatform.report.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProgressReportPdfModel(
        Instant periodStartedAt,
        Instant periodEndedAt,
        int learningMinutes,
        Metrics metrics,
        Assessment assessment,
        List<Topic> completedTopics,
        List<Topic> inProgressTopics,
        List<Skill> skills,
        String teacherSummary,
        String nextPeriodPlan) {
    public ProgressReportPdfModel {
        completedTopics = List.copyOf(completedTopics);
        inProgressTopics = List.copyOf(inProgressTopics);
        skills = List.copyOf(skills);
    }

    public record Metrics(
            int learningMinutes,
            long sessionsCount,
            double attendanceRate,
            long homeworkAssigned,
            long homeworkCompleted,
            long practiceAssigned,
            long practiceCompleted) {}

    public record Assessment(
            BigDecimal understandingAverage,
            BigDecimal independenceAverage,
            BigDecimal practiceAverage,
            BigDecimal homeworkAverage) {}

    public record Topic(String title) {}

    public record Skill(String name, BigDecimal progress) {}
}
