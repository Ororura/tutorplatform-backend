package com.tutorplatform.report.application;

import com.tutorplatform.session.application.LessonSessionChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class LessonSessionLearningPeriodListener {

    private static final Logger log = LoggerFactory.getLogger(
        LessonSessionLearningPeriodListener.class
    );

    private final LearningPeriodService learningPeriodService;

    public LessonSessionLearningPeriodListener(LearningPeriodService learningPeriodService) {
        this.learningPeriodService = learningPeriodService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLessonSessionChanged(LessonSessionChangedEvent event) {
        if (!event.affectsLearningMinutes()) {
            return;
        }
        try {
            learningPeriodService.recalculateAfterSession(event);
        } catch (RuntimeException exception) {
            log.error("LearningPeriod recalculation failed after LessonSession change {}",
                event.lessonSessionId(), exception);
        }
    }
}
