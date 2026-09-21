package com.tutorplatform.progress.api.response;

import com.tutorplatform.progress.domain.CurrentProgress;
import com.tutorplatform.progress.domain.TopicProgress;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Public, parent-safe live current progress")
public record PublicCurrentProgressResponse(
        long totalLearningMinutes,
        long sessionsCount,
        @Schema(description = "Attendance ratio in the 0..1 range", example = "0.75")
                double attendanceRate,
        PublicTopicsResponse topics,
        PublicHomeworkResponse homework,
        PublicPracticeResponse practice,
        PublicAssessmentResponse assessment) {
    public static PublicCurrentProgressResponse from(CurrentProgress progress) {
        return new PublicCurrentProgressResponse(
                progress.totalLearningMinutes(),
                progress.sessionsCount(),
                progress.attendanceRate(),
                new PublicTopicsResponse(
                        progress.completedTopics().stream().map(PublicTopicResponse::from).toList(),
                        progress.inProgressTopics().stream()
                                .map(PublicTopicResponse::from)
                                .toList()),
                new PublicHomeworkResponse(
                        progress.homeworkAssigned(), progress.homeworkCompleted()),
                new PublicPracticeResponse(
                        progress.practiceAssigned(), progress.practiceCompleted()),
                new PublicAssessmentResponse(
                        progress.assessmentAverages().understanding(),
                        progress.assessmentAverages().independence(),
                        progress.assessmentAverages().practice(),
                        progress.assessmentAverages().homework()));
    }

    public record PublicTopicsResponse(
            List<PublicTopicResponse> completed, List<PublicTopicResponse> inProgress) {}

    public record PublicTopicResponse(String title) {
        private static PublicTopicResponse from(TopicProgress topic) {
            return new PublicTopicResponse(topic.title());
        }
    }

    public record PublicHomeworkResponse(long assigned, long completed) {}

    public record PublicPracticeResponse(long assigned, long completed) {}

    public record PublicAssessmentResponse(
            @Schema(types = {"number", "null"}) BigDecimal understandingAverage,
            @Schema(types = {"number", "null"}) BigDecimal independenceAverage,
            @Schema(types = {"number", "null"}) BigDecimal practiceAverage,
            @Schema(types = {"number", "null"}) BigDecimal homeworkAverage) {}
}
