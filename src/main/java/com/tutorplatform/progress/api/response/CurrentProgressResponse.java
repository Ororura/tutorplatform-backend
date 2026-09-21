package com.tutorplatform.progress.api.response;

import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressStatus;
import com.tutorplatform.progress.domain.CurrentProgress;
import com.tutorplatform.progress.domain.TopicProgress;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Current learning progress for a student program")
public record CurrentProgressResponse(
        @Schema(format = "uuid") UUID studentProgramId,
        long totalLearningMinutes,
        long sessionsCount,
        @Schema(description = "Attendance ratio in the 0..1 range", example = "0.75")
                double attendanceRate,
        int totalTopics,
        TopicsResponse topics,
        HomeworkResponse homework,
        PracticeResponse practice,
        AssessmentResponse assessment) {
    public static CurrentProgressResponse from(CurrentProgress progress) {
        return new CurrentProgressResponse(
                progress.studentProgramId(),
                progress.totalLearningMinutes(),
                progress.sessionsCount(),
                progress.attendanceRate(),
                progress.totalTopics(),
                new TopicsResponse(
                        progress.completedTopics().stream().map(TopicResponse::from).toList(),
                        progress.inProgressTopics().stream().map(TopicResponse::from).toList()),
                new HomeworkResponse(progress.homeworkAssigned(), progress.homeworkCompleted()),
                new PracticeResponse(progress.practiceAssigned(), progress.practiceCompleted()),
                new AssessmentResponse(
                        progress.assessmentAverages().understanding(),
                        progress.assessmentAverages().independence(),
                        progress.assessmentAverages().practice(),
                        progress.assessmentAverages().homework()));
    }

    public record TopicsResponse(List<TopicResponse> completed, List<TopicResponse> inProgress) {}

    public record TopicResponse(
            @Schema(format = "uuid") UUID id, String title, StudentTopicProgressStatus status) {
        private static TopicResponse from(TopicProgress topic) {
            return new TopicResponse(topic.topicId(), topic.title(), topic.status());
        }
    }

    public record HomeworkResponse(long assigned, long completed) {}

    public record PracticeResponse(long assigned, long completed) {}

    public record AssessmentResponse(
            @Schema(types = {"number", "null"}) BigDecimal understandingAverage,
            @Schema(types = {"number", "null"}) BigDecimal independenceAverage,
            @Schema(types = {"number", "null"}) BigDecimal practiceAverage,
            @Schema(types = {"number", "null"}) BigDecimal homeworkAverage) {}
}
