package com.tutorplatform.report.application;

import com.tutorplatform.session.application.LessonSessionChangedEvent;
import com.tutorplatform.session.domain.AttendanceStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

class LessonSessionLearningPeriodListenerTest {

    private final LearningPeriodService service = mock(LearningPeriodService.class);
    private final LessonSessionLearningPeriodListener listener =
        new LessonSessionLearningPeriodListener(service);

    @Test
    void attendedCreateTriggersRecalculation() {
        LessonSessionChangedEvent event = created(AttendanceStatus.ATTENDED);

        listener.onLessonSessionChanged(event);

        verify(service).recalculateAfterSession(event);
    }

    @Test
    void missedAndCancelledCreatesDoNotTriggerRecalculation() {
        listener.onLessonSessionChanged(created(AttendanceStatus.MISSED));
        listener.onLessonSessionChanged(created(AttendanceStatus.CANCELLED));

        verifyNoInteractions(service);
    }

    @Test
    void attendedRelevantUpdateTriggersRecalculation() {
        Instant startedAt = Instant.parse("2026-01-01T10:00:00Z");
        LessonSessionChangedEvent event = new LessonSessionChangedEvent(
            UUID.randomUUID(), UUID.randomUUID(), AttendanceStatus.MISSED,
            startedAt, 90, AttendanceStatus.ATTENDED, startedAt, 60
        );

        listener.onLessonSessionChanged(event);

        verify(service).recalculateAfterSession(event);
    }

    @Test
    void listenerIsolatesAfterCommitFailure() {
        LessonSessionChangedEvent event = created(AttendanceStatus.ATTENDED);
        doThrow(new IllegalStateException("failed")).when(service).recalculateAfterSession(event);

        assertThatCode(() -> listener.onLessonSessionChanged(event)).doesNotThrowAnyException();
    }

    private LessonSessionChangedEvent created(AttendanceStatus status) {
        return new LessonSessionChangedEvent(
            UUID.randomUUID(), UUID.randomUUID(), status,
            Instant.parse("2026-01-01T10:00:00Z"), 60, null, null, null
        );
    }
}
