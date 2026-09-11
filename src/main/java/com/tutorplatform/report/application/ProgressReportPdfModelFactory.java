package com.tutorplatform.report.application;

import com.tutorplatform.report.application.exception.ProgressReportPdfNotAvailableException;
import com.tutorplatform.report.domain.ProgressReport;
import com.tutorplatform.report.domain.ProgressReportSnapshotSchemas;
import com.tutorplatform.report.domain.ProgressReportSnapshotV1;
import com.tutorplatform.report.domain.ProgressReportStatus;
import com.tutorplatform.report.domain.UnsupportedProgressReportSnapshotException;
import org.springframework.stereotype.Component;

@Component
public class ProgressReportPdfModelFactory {

    public ProgressReportPdfModel fromPublished(ProgressReport report) {
        if (report.status() != ProgressReportStatus.PUBLISHED) {
            throw new ProgressReportPdfNotAvailableException();
        }
        if (report.snapshotSchemaVersion() != ProgressReportSnapshotSchemas.V1) {
            throw new UnsupportedProgressReportSnapshotException(report.snapshotSchemaVersion());
        }
        return fromV1(report);
    }

    private ProgressReportPdfModel fromV1(ProgressReport report) {
        ProgressReportSnapshotV1 snapshot = report.snapshot();
        ProgressReportSnapshotV1.Metrics metrics = snapshot.metrics();
        ProgressReportSnapshotV1.Assessment assessment = snapshot.assessment();
        return new ProgressReportPdfModel(
            report.periodStartedAt(),
            report.periodEndedAt(),
            report.learningMinutes(),
            new ProgressReportPdfModel.Metrics(
                metrics.learningMinutes(), metrics.sessionsCount(), metrics.attendanceRate(),
                metrics.homeworkAssigned(), metrics.homeworkCompleted(),
                metrics.practiceAssigned(), metrics.practiceCompleted()
            ),
            new ProgressReportPdfModel.Assessment(
                assessment.understandingAverage(), assessment.independenceAverage(),
                assessment.practiceAverage(), assessment.homeworkAverage()
            ),
            snapshot.topics().completed().stream()
                .map(topic -> new ProgressReportPdfModel.Topic(topic.title())).toList(),
            snapshot.topics().inProgress().stream()
                .map(topic -> new ProgressReportPdfModel.Topic(topic.title())).toList(),
            snapshot.skills().stream()
                .map(skill -> new ProgressReportPdfModel.Skill(skill.name(), skill.progress())).toList(),
            report.teacherSummary(),
            report.nextPeriodPlan()
        );
    }
}
