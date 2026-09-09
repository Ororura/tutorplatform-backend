package com.tutorplatform.homework.application;

import com.tutorplatform.submission.application.SubmissionReviewedEvent;
import com.tutorplatform.submission.domain.SubmissionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SubmissionReviewedHomeworkListener {

    private static final Logger log = LoggerFactory.getLogger(SubmissionReviewedHomeworkListener.class);

    private final HomeworkCompletionService completionService;

    public SubmissionReviewedHomeworkListener(HomeworkCompletionService completionService) {
        this.completionService = completionService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmissionReviewed(SubmissionReviewedEvent event) {
        if (event.resultingStatus() != SubmissionStatus.PASSED || event.homeworkItemId() == null) {
            return;
        }
        try {
            completionService.recalculate(
                event.homeworkItemId(), event.studentId(), event.studentProgramId(), event.taskId()
            );
        } catch (RuntimeException exception) {
            log.error("Homework completion recalculation failed after submission review {}",
                event.submissionId(), exception);
        }
    }
}
