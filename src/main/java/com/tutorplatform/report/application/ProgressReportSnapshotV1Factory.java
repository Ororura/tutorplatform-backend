package com.tutorplatform.report.application;

import com.tutorplatform.progress.domain.CurrentProgress;
import com.tutorplatform.progress.domain.TopicProgress;
import com.tutorplatform.report.domain.ProgressReportSnapshotV1;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProgressReportSnapshotV1Factory {

    public ProgressReportSnapshotV1 create(CurrentProgress progress, int learningMinutes) {
        return new ProgressReportSnapshotV1(
            new ProgressReportSnapshotV1.Metrics(
                learningMinutes,
                progress.sessionsCount(),
                progress.attendanceRate(),
                progress.homeworkAssigned(),
                progress.homeworkCompleted(),
                progress.practiceAssigned(),
                progress.practiceCompleted()
            ),
            new ProgressReportSnapshotV1.Assessment(
                progress.assessmentAverages().understanding(),
                progress.assessmentAverages().independence(),
                progress.assessmentAverages().practice(),
                progress.assessmentAverages().homework()
            ),
            new ProgressReportSnapshotV1.Topics(
                topics(progress.completedTopics()),
                topics(progress.inProgressTopics())
            ),
            List.of()
        );
    }

    private List<ProgressReportSnapshotV1.Topic> topics(List<TopicProgress> topics) {
        return topics.stream()
            .map(topic -> new ProgressReportSnapshotV1.Topic(topic.topicId(), topic.title()))
            .toList();
    }
}
