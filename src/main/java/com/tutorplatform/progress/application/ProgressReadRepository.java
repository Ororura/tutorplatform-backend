package com.tutorplatform.progress.application;

import com.tutorplatform.progress.domain.*;

import java.util.List;
import java.util.UUID;

public interface ProgressReadRepository {

    SessionMetrics getSessionMetrics(UUID studentProgramId);

    List<TopicProgress> findTopicProgress(UUID studentProgramId);

    HomeworkMetrics getHomeworkMetrics(UUID studentProgramId);

    PracticeMetrics getPracticeMetrics(UUID studentProgramId);

    AssessmentAverages getAssessmentAverages(UUID studentProgramId);

    SessionMetrics getSessionMetrics(UUID studentProgramId, ProgressInterval interval);

    List<TopicProgress> findTopicProgress(UUID studentProgramId, ProgressInterval interval);

    HomeworkMetrics getHomeworkMetrics(UUID studentProgramId, ProgressInterval interval);

    PracticeMetrics getPracticeMetrics(UUID studentProgramId, ProgressInterval interval);

    AssessmentAverages getAssessmentAverages(UUID studentProgramId, ProgressInterval interval);
}
