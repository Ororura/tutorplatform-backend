package com.tutorplatform.progress.domain;

import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ProgressCalculator {

    public CurrentProgress calculate(
        UUID studentProgramId,
        SessionMetrics sessions,
        List<TopicProgress> topics,
        HomeworkMetrics homework,
        PracticeMetrics practice,
        AssessmentAverages assessments
    ) {
        List<TopicProgress> completedTopics = topics.stream()
            .filter(topic -> topic.status() == StudentTopicProgressStatus.COMPLETED)
            .toList();
        List<TopicProgress> inProgressTopics = topics.stream()
            .filter(topic -> topic.status() == StudentTopicProgressStatus.IN_PROGRESS)
            .toList();

        return new CurrentProgress(
            studentProgramId,
            sessions.totalLearningMinutes(),
            sessions.sessionsCount(),
            attendanceRate(sessions.attendedCount(), sessions.missedCount()),
            topics.size(),
            completedTopics,
            inProgressTopics,
            homework.assigned(),
            homework.completed(),
            practice.assigned(),
            practice.completed(),
            assessments
        );
    }

    static double attendanceRate(long attended, long missed) {
        long denominator = attended + missed;
        return denominator == 0 ? 0.0 : (double) attended / denominator;
    }
}
