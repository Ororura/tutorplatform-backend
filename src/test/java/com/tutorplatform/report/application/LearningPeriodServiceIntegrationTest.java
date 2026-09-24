package com.tutorplatform.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tutorplatform.report.application.exception.HistoricalLearningPeriodChangeException;
import com.tutorplatform.report.domain.LearningPeriod;
import com.tutorplatform.report.domain.LearningPeriodStatus;
import com.tutorplatform.session.application.LessonSessionChangedEvent;
import com.tutorplatform.session.domain.AttendanceStatus;
import com.tutorplatform.test.PostgresIntegrationTest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class LearningPeriodServiceIntegrationTest extends PostgresIntegrationTest {

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_learning_period_service", "008");
    }

    private static final Instant BASE = Instant.parse("2026-01-01T10:00:00Z");

    @Autowired private LearningPeriodService service;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void firstPeriodUsesProgramIntervalAndEnsureIsIdempotent() {
        Fixture fixture = fixture(480);

        LearningPeriod first = service.ensureActivePeriod(fixture.studentProgramId());
        LearningPeriod repeated = service.ensureActivePeriod(fixture.studentProgramId());

        assertThat(first.sequenceNo()).isOne();
        assertThat(first.startCumulativeMinutes()).isZero();
        assertThat(first.targetDurationMinutes()).isEqualTo(480);
        assertThat(first.status()).isEqualTo(LearningPeriodStatus.ACTIVE);
        assertThat(first.startedAt()).isNull();
        assertThat(repeated.id()).isEqualTo(first.id());
        assertThat(activeCount(fixture)).isOne();
        assertThat(service.listLearningPeriods(fixture.studentProgramId())).hasSize(1);
    }

    @Test
    void missedAndCancelledDoNotStartOrIncreasePeriod() {
        Fixture fixture = fixture(480);
        service.ensureActivePeriod(fixture.studentProgramId());
        SessionFact missed = session(fixture, AttendanceStatus.MISSED, 120, BASE);
        service.recalculateAfterSession(missed.createdEvent());
        SessionFact cancelled =
                session(fixture, AttendanceStatus.CANCELLED, 180, BASE.plusSeconds(3600));
        service.recalculateAfterSession(cancelled.createdEvent());

        LearningPeriod active = only(fixture);
        assertThat(active.startedAt()).isNull();
        assertThat(active.status()).isEqualTo(LearningPeriodStatus.ACTIVE);
        assertThat(totalLearningMinutes(fixture)).isZero();
    }

    @Test
    void firstAttendedStartsPeriodAndTotalsBelowThresholdStayActive() {
        Fixture fixture = fixture(480);
        service.ensureActivePeriod(fixture.studentProgramId());
        SessionFact first = session(fixture, AttendanceStatus.ATTENDED, 60, BASE);
        service.recalculateAfterSession(first.createdEvent());
        assertThat(only(fixture).status()).isEqualTo(LearningPeriodStatus.ACTIVE);
        SessionFact second =
                session(fixture, AttendanceStatus.ATTENDED, 419, BASE.plusSeconds(3600));
        service.recalculateAfterSession(second.createdEvent());

        LearningPeriod active = only(fixture);
        assertThat(active.startedAt()).isEqualTo(BASE);
        assertThat(active.status()).isEqualTo(LearningPeriodStatus.ACTIVE);
        assertThat(totalLearningMinutes(fixture)).isEqualTo(479);
    }

    @Test
    void overshootAt510BecomesNextPeriodStartAndUses990Threshold() {
        Fixture fixture = fixture(480);
        service.ensureActivePeriod(fixture.studentProgramId());
        SessionFact first = session(fixture, AttendanceStatus.ATTENDED, 450, BASE);
        service.recalculateAfterSession(first.createdEvent());
        Instant crossingAt = BASE.plusSeconds(3600);
        SessionFact crossing = session(fixture, AttendanceStatus.ATTENDED, 60, crossingAt);

        service.recalculateAfterSession(crossing.createdEvent());

        List<LearningPeriod> periods = periods(fixture);
        assertThat(periods.get(0).endCumulativeMinutes()).isEqualTo(510);
        assertThat(periods.get(0).completedAt()).isEqualTo(crossingAt);
        assertThat(periods.get(1).startCumulativeMinutes()).isEqualTo(510);
        assertThat(periods.get(1).thresholdMinutes()).isEqualTo(990);
    }

    @Test
    void exactThresholdCompletesAtCrossingSessionAndCreatesNextPeriod() {
        Fixture fixture = fixture(480);
        service.ensureActivePeriod(fixture.studentProgramId());
        SessionFact first = session(fixture, AttendanceStatus.ATTENDED, 420, BASE);
        service.recalculateAfterSession(first.createdEvent());
        Instant crossingAt = BASE.plusSeconds(3600);
        SessionFact crossing = session(fixture, AttendanceStatus.ATTENDED, 60, crossingAt);
        service.recalculateAfterSession(crossing.createdEvent());

        List<LearningPeriod> periods = periods(fixture);
        assertThat(periods).hasSize(2);
        assertThat(periods.get(0).status()).isEqualTo(LearningPeriodStatus.COMPLETED);
        assertThat(periods.get(0).endCumulativeMinutes()).isEqualTo(480);
        assertThat(periods.get(0).completedAt()).isEqualTo(crossingAt);
        assertThat(periods.get(1).sequenceNo()).isEqualTo(2);
        assertThat(periods.get(1).startCumulativeMinutes()).isEqualTo(480);
        assertThat(periods.get(1).status()).isEqualTo(LearningPeriodStatus.ACTIVE);
        assertThat(periods.get(1).thresholdMinutes()).isEqualTo(960);
        assertThat(activeCount(fixture)).isOne();
    }

    @Test
    void consecutiveCompletedPeriodsCountEachSessionOnce() {
        Fixture fixture = fixture(60);
        service.ensureActivePeriod(fixture.studentProgramId());
        SessionFact first = session(fixture, AttendanceStatus.ATTENDED, 60, BASE);
        service.recalculateAfterSession(first.createdEvent());
        SessionFact second =
                session(fixture, AttendanceStatus.ATTENDED, 60, BASE.plusSeconds(3600));
        service.recalculateAfterSession(second.createdEvent());

        List<LearningPeriod> periods = periods(fixture);
        assertThat(periods).hasSize(3);
        assertThat(periods.get(0).endCumulativeMinutes() - periods.get(0).startCumulativeMinutes())
                .isEqualTo(60);
        assertThat(periods.get(1).endCumulativeMinutes() - periods.get(1).startCumulativeMinutes())
                .isEqualTo(60);
        assertThat(periods.get(1).startCumulativeMinutes())
                .isEqualTo(periods.get(0).endCumulativeMinutes());
        assertThat(periods.get(2).startCumulativeMinutes())
                .isEqualTo(totalLearningMinutes(fixture));
        assertThat(activeCount(fixture)).isOne();
    }

    @Test
    void sessionOvershootIsNotSplitAtNominalBoundary() {
        Fixture fixture = fixture(480);
        service.ensureActivePeriod(fixture.studentProgramId());
        SessionFact first = session(fixture, AttendanceStatus.ATTENDED, 420, BASE);
        service.recalculateAfterSession(first.createdEvent());
        Instant crossingAt = BASE.plusSeconds(3600);
        SessionFact crossing = session(fixture, AttendanceStatus.ATTENDED, 120, crossingAt);
        service.recalculateAfterSession(crossing.createdEvent());

        List<LearningPeriod> periods = periods(fixture);
        assertThat(periods.get(0).endCumulativeMinutes()).isEqualTo(540);
        assertThat(periods.get(0).completedAt()).isEqualTo(crossingAt);
        assertThat(periods.get(1).startCumulativeMinutes()).isEqualTo(540);
        assertThat(periods.get(1).thresholdMinutes()).isEqualTo(1020);
        assertThat(periods)
                .noneMatch(period -> Integer.valueOf(480).equals(period.endCumulativeMinutes()));
    }

    @Test
    void oneLargeSessionCompletesOnlyOnePeriod() {
        Fixture fixture = fixture(60);
        service.ensureActivePeriod(fixture.studentProgramId());
        SessionFact session = session(fixture, AttendanceStatus.ATTENDED, 180, BASE);

        service.recalculateAfterSession(session.createdEvent());

        List<LearningPeriod> periods = periods(fixture);
        assertThat(periods).hasSize(2);
        assertThat(periods.get(0).endCumulativeMinutes()).isEqualTo(180);
        assertThat(periods.get(1).startCumulativeMinutes()).isEqualTo(180);
        assertThat(periods.get(1).status()).isEqualTo(LearningPeriodStatus.ACTIVE);
    }

    @Test
    void multipleAttendedSessionsAreSummedWhileOtherAttendanceIsIgnored() {
        Fixture fixture = fixture(300);
        service.ensureActivePeriod(fixture.studentProgramId());
        SessionFact attended60 = session(fixture, AttendanceStatus.ATTENDED, 60, BASE);
        service.recalculateAfterSession(attended60.createdEvent());
        SessionFact missed = session(fixture, AttendanceStatus.MISSED, 90, BASE.plusSeconds(3600));
        service.recalculateAfterSession(missed.createdEvent());
        SessionFact cancelled =
                session(fixture, AttendanceStatus.CANCELLED, 120, BASE.plusSeconds(7200));
        service.recalculateAfterSession(cancelled.createdEvent());
        SessionFact attended90 =
                session(fixture, AttendanceStatus.ATTENDED, 90, BASE.plusSeconds(10800));
        service.recalculateAfterSession(attended90.createdEvent());

        assertThat(totalLearningMinutes(fixture)).isEqualTo(150);
        assertThat(only(fixture).status()).isEqualTo(LearningPeriodStatus.ACTIVE);
    }

    @Test
    void intervalChangeAffectsOnlyNextPeriod() {
        Fixture fixture = fixture(480);
        LearningPeriod first = service.ensureActivePeriod(fixture.studentProgramId());
        jdbc.update(
                "UPDATE student_programs SET report_interval_minutes = 600 WHERE id = ?",
                fixture.studentProgramId());
        SessionFact crossing = session(fixture, AttendanceStatus.ATTENDED, 480, BASE);

        service.recalculateAfterSession(crossing.createdEvent());

        List<LearningPeriod> periods = periods(fixture);
        assertThat(periods.get(0).id()).isEqualTo(first.id());
        assertThat(periods.get(0).targetDurationMinutes()).isEqualTo(480);
        assertThat(periods.get(1).targetDurationMinutes()).isEqualTo(600);
    }

    @Test
    void duplicateSessionEventDoesNotCompleteOrAdvanceAgain() {
        Fixture fixture = fixture(60);
        service.ensureActivePeriod(fixture.studentProgramId());
        SessionFact crossing = session(fixture, AttendanceStatus.ATTENDED, 60, BASE);
        service.recalculateAfterSession(crossing.createdEvent());
        LearningPeriod completed = periods(fixture).get(0);

        service.recalculateAfterSession(crossing.createdEvent());

        List<LearningPeriod> periods = periods(fixture);
        assertThat(periods).hasSize(2);
        assertThat(periods.get(0).completedAt()).isEqualTo(completed.completedAt());
        assertThat(periods.get(1).sequenceNo()).isEqualTo(2);
        assertThat(activeCount(fixture)).isOne();
    }

    @Test
    void updateWithinActivePeriodRecalculatesDurationAndStartedAt() {
        Fixture fixture = fixture(480);
        service.ensureActivePeriod(fixture.studentProgramId());
        SessionFact original = session(fixture, AttendanceStatus.ATTENDED, 60, BASE);
        service.recalculateAfterSession(original.createdEvent());
        Instant moved = BASE.plusSeconds(7200);
        jdbc.update(
                "UPDATE lesson_sessions SET duration_minutes = 90, started_at = ? WHERE id = ?",
                Timestamp.from(moved),
                original.id());
        LessonSessionChangedEvent update =
                new LessonSessionChangedEvent(
                        original.id(),
                        fixture.studentProgramId(),
                        AttendanceStatus.ATTENDED,
                        moved,
                        90,
                        AttendanceStatus.ATTENDED,
                        BASE,
                        60);

        service.recalculateAfterSession(update);

        assertThat(totalLearningMinutes(fixture)).isEqualTo(90);
        assertThat(only(fixture).startedAt()).isEqualTo(moved);
    }

    @Test
    void historicalMutationThatInvalidatesCompletedBoundaryIsRejected() {
        Fixture fixture = fixture(60);
        service.ensureActivePeriod(fixture.studentProgramId());
        SessionFact crossing = session(fixture, AttendanceStatus.ATTENDED, 60, BASE);
        service.recalculateAfterSession(crossing.createdEvent());
        jdbc.update("UPDATE lesson_sessions SET duration_minutes = 30 WHERE id = ?", crossing.id());
        LessonSessionChangedEvent update =
                new LessonSessionChangedEvent(
                        crossing.id(),
                        fixture.studentProgramId(),
                        AttendanceStatus.ATTENDED,
                        BASE,
                        30,
                        AttendanceStatus.ATTENDED,
                        BASE,
                        60);

        assertThatThrownBy(() -> service.recalculateAfterSession(update))
                .isInstanceOf(HistoricalLearningPeriodChangeException.class);
        assertThat(periods(fixture)).hasSize(2);
    }

    @Test
    void concurrentEnsureKeepsSingleActivePeriod() throws Exception {
        Fixture fixture = fixture(480);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first =
                    executor.submit(
                            () -> {
                                start.await();
                                return service.ensureActivePeriod(fixture.studentProgramId());
                            });
            var second =
                    executor.submit(
                            () -> {
                                start.await();
                                return service.ensureActivePeriod(fixture.studentProgramId());
                            });
            start.countDown();

            assertThat(first.get(10, TimeUnit.SECONDS).id())
                    .isEqualTo(second.get(10, TimeUnit.SECONDS).id());
        }
        assertThat(activeCount(fixture)).isOne();
    }

    @Test
    void concurrentRecalculationsCompleteOnceAndKeepSingleActivePeriod() throws Exception {
        Fixture fixture = fixture(60);
        service.ensureActivePeriod(fixture.studentProgramId());
        SessionFact firstSession = session(fixture, AttendanceStatus.ATTENDED, 30, BASE);
        SessionFact secondSession =
                session(fixture, AttendanceStatus.ATTENDED, 30, BASE.plusSeconds(3600));
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first =
                    executor.submit(
                            () -> {
                                start.await();
                                return service.recalculateAfterSession(firstSession.createdEvent());
                            });
            var second =
                    executor.submit(
                            () -> {
                                start.await();
                                return service.recalculateAfterSession(
                                        secondSession.createdEvent());
                            });
            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        }

        List<LearningPeriod> periods = periods(fixture);
        assertThat(periods).hasSize(2);
        assertThat(periods.get(0).status()).isEqualTo(LearningPeriodStatus.COMPLETED);
        assertThat(periods.get(0).endCumulativeMinutes()).isEqualTo(60);
        assertThat(periods.get(1).status()).isEqualTo(LearningPeriodStatus.ACTIVE);
        assertThat(activeCount(fixture)).isOne();
    }

    private Fixture fixture(int interval) {
        UUID userId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID learningProgramId = UUID.randomUUID();
        UUID studentProgramId = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id, email) VALUES (?, ?)", userId, userId + "@example.com");
        jdbc.update(
                "INSERT INTO teachers(id, user_id, display_name) VALUES (?, ?, 'Teacher')",
                teacherId,
                userId);
        jdbc.update("INSERT INTO students(id, first_name) VALUES (?, 'Student')", studentId);
        jdbc.update(
                "INSERT INTO subjects(id, owner_teacher_id, name) VALUES (?, ?, ?)",
                subjectId,
                teacherId,
                "Subject " + subjectId);
        jdbc.update(
                """
            INSERT INTO learning_programs(id, teacher_id, subject_id, title, status)
            VALUES (?, ?, ?, 'Program', 'ACTIVE')
            """,
                learningProgramId,
                teacherId,
                subjectId);
        jdbc.update(
                """
            INSERT INTO student_programs(
                id, student_id, learning_program_id, assigned_by_teacher_id,
                report_interval_minutes
            ) VALUES (?, ?, ?, ?, ?)
            """,
                studentProgramId,
                studentId,
                learningProgramId,
                teacherId,
                interval);
        return new Fixture(teacherId, studentProgramId);
    }

    private SessionFact session(
            Fixture fixture, AttendanceStatus status, int durationMinutes, Instant startedAt) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                """
            INSERT INTO lesson_sessions(
                id, student_program_id, teacher_id, started_at,
                duration_minutes, attendance_status
            ) VALUES (?, ?, ?, ?, ?, ?)
            """,
                id,
                fixture.studentProgramId(),
                fixture.teacherId(),
                Timestamp.from(startedAt),
                durationMinutes,
                status.name());
        return new SessionFact(id, fixture.studentProgramId(), status, durationMinutes, startedAt);
    }

    private List<LearningPeriod> periods(Fixture fixture) {
        return service.listLearningPeriods(fixture.studentProgramId());
    }

    private LearningPeriod only(Fixture fixture) {
        return periods(fixture).getFirst();
    }

    private int activeCount(Fixture fixture) {
        return jdbc.queryForObject(
                """
            SELECT COUNT(*) FROM learning_periods
            WHERE student_program_id = ? AND status = 'ACTIVE'
            """,
                Integer.class,
                fixture.studentProgramId());
    }

    private int totalLearningMinutes(Fixture fixture) {
        return jdbc.queryForObject(
                """
            SELECT COALESCE(SUM(duration_minutes), 0) FROM lesson_sessions
            WHERE student_program_id = ? AND attendance_status = 'ATTENDED'
            """,
                Integer.class,
                fixture.studentProgramId());
    }

    private record Fixture(UUID teacherId, UUID studentProgramId) {}

    private record SessionFact(
            UUID id,
            UUID studentProgramId,
            AttendanceStatus status,
            int durationMinutes,
            Instant startedAt) {
        LessonSessionChangedEvent createdEvent() {
            return new LessonSessionChangedEvent(
                    id, studentProgramId, status, startedAt, durationMinutes, null, null, null);
        }
    }
}
