package com.tutorplatform.progress.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProgressCalculatorTest {

    private static final UUID STUDENT_PROGRAM_ID = UUID.randomUUID();
    private final ProgressCalculator calculator = new ProgressCalculator();

    @Test
    void oneAttendedSessionContributesItsMinutes() {
        assertThat(progress(new SessionMetrics(60, 1, 0, 1), List.of()).totalLearningMinutes())
                .isEqualTo(60);
    }

    @Test
    void attendedSessionMinutesAreSummed() {
        assertThat(progress(new SessionMetrics(150, 2, 0, 2), List.of()).totalLearningMinutes())
                .isEqualTo(150);
    }

    @Test
    void missedSessionDoesNotContributeMinutes() {
        assertThat(progress(new SessionMetrics(0, 0, 1, 1), List.of()).totalLearningMinutes())
                .isZero();
    }

    @Test
    void cancelledSessionDoesNotContributeMinutesOrSessionCount() {
        CurrentProgress progress = progress(new SessionMetrics(0, 0, 0, 0), List.of());

        assertThat(progress.totalLearningMinutes()).isZero();
        assertThat(progress.sessionsCount()).isZero();
    }

    @Test
    void attendanceIsAttendedDividedByAttendedAndMissed() {
        assertThat(progress(new SessionMetrics(120, 2, 1, 3), List.of()).attendanceRate())
                .isCloseTo(2.0 / 3.0, within(1.0e-12));
    }

    @Test
    void cancelledSessionDoesNotChangeAttendanceDenominator() {
        assertThat(progress(new SessionMetrics(60, 1, 0, 1), List.of()).attendanceRate())
                .isEqualTo(1.0);
    }

    @Test
    void emptySessionsUseZeroAttendanceRate() {
        CurrentProgress progress = progress(new SessionMetrics(0, 0, 0, 0), List.of());

        assertThat(progress.sessionsCount()).isZero();
        assertThat(progress.attendanceRate()).isZero();
    }

    @Test
    void completedTopicAppearsOnlyInCompletedTopics() {
        TopicProgress topic = topic("Completed", StudentTopicProgressStatus.COMPLETED);

        CurrentProgress progress = progress(new SessionMetrics(0, 0, 0, 0), List.of(topic));

        assertThat(progress.completedTopics()).containsExactly(topic);
        assertThat(progress.inProgressTopics()).isEmpty();
    }

    @Test
    void inProgressTopicAppearsOnlyInInProgressTopics() {
        TopicProgress topic = topic("In progress", StudentTopicProgressStatus.IN_PROGRESS);

        CurrentProgress progress = progress(new SessionMetrics(0, 0, 0, 0), List.of(topic));

        assertThat(progress.inProgressTopics()).containsExactly(topic);
        assertThat(progress.completedTopics()).isEmpty();
    }

    @Test
    void availableTopicIsNotCompleted() {
        CurrentProgress progress =
                progress(
                        new SessionMetrics(0, 0, 0, 0),
                        List.of(topic("Available", StudentTopicProgressStatus.AVAILABLE)));

        assertThat(progress.completedTopics()).isEmpty();
        assertThat(progress.totalTopics()).isOne();
    }

    @Test
    void lockedTopicIsNotCompleted() {
        CurrentProgress progress =
                progress(
                        new SessionMetrics(0, 0, 0, 0),
                        List.of(topic("Locked", StudentTopicProgressStatus.LOCKED)));

        assertThat(progress.completedTopics()).isEmpty();
        assertThat(progress.totalTopics()).isOne();
    }

    private CurrentProgress progress(SessionMetrics sessions, List<TopicProgress> topics) {
        return calculator.calculate(
                STUDENT_PROGRAM_ID,
                sessions,
                topics,
                new HomeworkMetrics(0, 0),
                new PracticeMetrics(0, 0),
                new AssessmentAverages(null, null, null, null));
    }

    private TopicProgress topic(String title, StudentTopicProgressStatus status) {
        return new TopicProgress(UUID.randomUUID(), title, status);
    }

    private static org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }
}
