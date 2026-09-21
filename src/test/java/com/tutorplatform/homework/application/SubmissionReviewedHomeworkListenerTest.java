package com.tutorplatform.homework.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

import com.tutorplatform.submission.application.SubmissionReviewedEvent;
import com.tutorplatform.submission.domain.SubmissionStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubmissionReviewedHomeworkListenerTest {

    private final HomeworkCompletionService completionService =
            mock(HomeworkCompletionService.class);
    private final SubmissionReviewedHomeworkListener listener =
            new SubmissionReviewedHomeworkListener(completionService);

    @Test
    void passedHomeworkSubmissionTriggersRecalculation() {
        SubmissionReviewedEvent event = event(UUID.randomUUID(), SubmissionStatus.PASSED);

        listener.onSubmissionReviewed(event);

        verify(completionService)
                .recalculate(
                        event.homeworkItemId(),
                        event.studentId(),
                        event.studentProgramId(),
                        event.taskId());
    }

    @Test
    void failedReviewDoesNotTriggerRecalculation() {
        listener.onSubmissionReviewed(event(UUID.randomUUID(), SubmissionStatus.FAILED));

        verifyNoInteractions(completionService);
    }

    @Test
    void passedPracticeSubmissionDoesNotTriggerRecalculation() {
        listener.onSubmissionReviewed(event(null, SubmissionStatus.PASSED));

        verifyNoInteractions(completionService);
    }

    @Test
    void repeatedEventHandlingIsSafeToDelegate() {
        SubmissionReviewedEvent event = event(UUID.randomUUID(), SubmissionStatus.PASSED);

        listener.onSubmissionReviewed(event);
        listener.onSubmissionReviewed(event);

        verify(completionService, times(2))
                .recalculate(
                        event.homeworkItemId(),
                        event.studentId(),
                        event.studentProgramId(),
                        event.taskId());
    }

    @Test
    void completionFailureIsIsolatedFromAfterCommitEventHandling() {
        SubmissionReviewedEvent event = event(UUID.randomUUID(), SubmissionStatus.PASSED);
        doThrow(new IllegalStateException("completion failed"))
                .when(completionService)
                .recalculate(
                        event.homeworkItemId(),
                        event.studentId(),
                        event.studentProgramId(),
                        event.taskId());

        assertThatCode(() -> listener.onSubmissionReviewed(event)).doesNotThrowAnyException();
    }

    private SubmissionReviewedEvent event(UUID homeworkItemId, SubmissionStatus status) {
        return new SubmissionReviewedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                homeworkItemId,
                status);
    }
}
