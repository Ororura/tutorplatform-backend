package com.tutorplatform.submission.application;

import com.tutorplatform.submission.domain.SubmissionStatus;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Public read contract for consumers that need submission completion facts. */
public interface SubmissionQuery {

    Set<PassedHomeworkItem> findPassedHomeworkItems(
            UUID studentId, UUID studentProgramId, Set<UUID> homeworkItemIds);

    Map<UUID, HomeworkItemSubmissionState> findHomeworkItemStates(
            UUID studentId, UUID studentProgramId, Set<UUID> homeworkItemIds);

    record PassedHomeworkItem(UUID homeworkItemId, UUID taskId) {}

    record HomeworkItemSubmissionState(boolean passed, SubmissionStatus latestSubmissionStatus) {}
}
