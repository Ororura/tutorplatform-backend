package com.tutorplatform.progress.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CurrentProgress(
        UUID studentProgramId,
        long totalLearningMinutes,
        long sessionsCount,
        double attendanceRate,
        int totalTopics,
        List<TopicProgress> completedTopics,
        List<TopicProgress> inProgressTopics,
        long homeworkAssigned,
        long homeworkCompleted,
        long practiceAssigned,
        long practiceCompleted,
        AssessmentAverages assessmentAverages) {
    public CurrentProgress {
        Objects.requireNonNull(studentProgramId);
        completedTopics = List.copyOf(completedTopics);
        inProgressTopics = List.copyOf(inProgressTopics);
        Objects.requireNonNull(assessmentAverages);
    }
}
