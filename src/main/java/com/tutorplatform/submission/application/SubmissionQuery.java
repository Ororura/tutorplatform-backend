package com.tutorplatform.submission.application;

import java.util.Set;
import java.util.UUID;

/**
 * Public read contract for consumers that need submission completion facts.
 */
public interface SubmissionQuery {

    Set<PassedHomeworkItem> findPassedHomeworkItems(
        UUID studentId,
        UUID studentProgramId,
        Set<UUID> homeworkItemIds
    );

    record PassedHomeworkItem(UUID homeworkItemId, UUID taskId) {
    }
}
