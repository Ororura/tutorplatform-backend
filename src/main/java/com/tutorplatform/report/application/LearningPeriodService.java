package com.tutorplatform.report.application;

import com.tutorplatform.program.application.ProgramQuery;
import com.tutorplatform.progress.application.ProgressReadRepository;
import com.tutorplatform.report.application.exception.HistoricalLearningPeriodChangeException;
import com.tutorplatform.report.application.exception.LearningPeriodStudentProgramNotFoundException;
import com.tutorplatform.report.domain.LearningPeriod;
import com.tutorplatform.report.domain.LearningPeriodRepository;
import com.tutorplatform.report.domain.LearningPeriodStatus;
import com.tutorplatform.session.application.AttendedLessonSession;
import com.tutorplatform.session.application.LearningSessionQuery;
import com.tutorplatform.session.application.LessonSessionChangedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class LearningPeriodService {

    private final ProgramQuery programQuery;
    private final LearningPeriodRepository learningPeriodRepository;
    private final ProgressReadRepository progressReadRepository;
    private final LearningSessionQuery learningSessionQuery;

    public LearningPeriodService(
        ProgramQuery programQuery,
        LearningPeriodRepository learningPeriodRepository,
        ProgressReadRepository progressReadRepository,
        LearningSessionQuery learningSessionQuery
    ) {
        this.programQuery = programQuery;
        this.learningPeriodRepository = learningPeriodRepository;
        this.progressReadRepository = progressReadRepository;
        this.learningSessionQuery = learningSessionQuery;
    }

    @Transactional
    public LearningPeriod ensureActivePeriod(UUID studentProgramId) {
        ProgramQuery.StudentProgramContext studentProgram = lockStudentProgram(studentProgramId);
        return ensureActivePeriod(studentProgram);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LearningPeriod recalculateAfterSession(LessonSessionChangedEvent event) {
        ProgramQuery.StudentProgramContext studentProgram = lockStudentProgram(event.studentProgramId());
        LearningPeriod active = ensureActivePeriod(studentProgram);
        if (!event.affectsLearningMinutes()) {
            return active;
        }

        int totalLearningMinutes = Math.toIntExact(
            progressReadRepository.getSessionMetrics(studentProgram.id()).totalLearningMinutes()
        );
        List<AttendedLessonSession> attendedSessions =
            learningSessionQuery.findAttendedByStudentProgram(studentProgram.id());
        Timeline timeline = buildTimeline(active, attendedSessions, totalLearningMinutes);

        Instant now = Instant.now();
        LearningPeriod recalculated = active.withStartedAt(timeline.firstPeriodSessionAt(), now);
        if (totalLearningMinutes < recalculated.thresholdMinutes()) {
            return recalculated == active
                ? active
                : learningPeriodRepository.saveAndFlush(recalculated);
        }

        if (timeline.thresholdCrossedAt() == null) {
            throw new HistoricalLearningPeriodChangeException(studentProgram.id());
        }
        LearningPeriod completed = learningPeriodRepository.saveAndFlush(
            recalculated.complete(totalLearningMinutes, timeline.thresholdCrossedAt(), now)
        );
        learningPeriodRepository.saveAndFlush(LearningPeriod.active(
            UUID.randomUUID(),
            studentProgram.id(),
            completed.sequenceNo() + 1,
            completed.endCumulativeMinutes(),
            studentProgram.reportIntervalMinutes(),
            now
        ));
        return completed;
    }

    @Transactional(readOnly = true)
    public List<LearningPeriod> listLearningPeriods(UUID studentProgramId) {
        return learningPeriodRepository.findAllByStudentProgramIdOrderBySequenceNo(studentProgramId);
    }

    private ProgramQuery.StudentProgramContext lockStudentProgram(UUID studentProgramId) {
        return programQuery.findStudentProgramForUpdate(studentProgramId)
            .orElseThrow(() -> new LearningPeriodStudentProgramNotFoundException(studentProgramId));
    }

    private LearningPeriod ensureActivePeriod(ProgramQuery.StudentProgramContext studentProgram) {
        return learningPeriodRepository.findActiveByStudentProgramIdForUpdate(studentProgram.id())
            .orElseGet(() -> {
                LearningPeriod latest = learningPeriodRepository
                    .findLatestByStudentProgramId(studentProgram.id())
                    .orElse(null);
                int sequenceNo = latest == null ? 1 : latest.sequenceNo() + 1;
                int startMinutes = latest == null ? 0 : completedEnd(latest);
                Instant now = Instant.now();
                return learningPeriodRepository.saveAndFlush(LearningPeriod.active(
                    UUID.randomUUID(), studentProgram.id(), sequenceNo, startMinutes,
                    studentProgram.reportIntervalMinutes(), now
                ));
            });
    }

    private int completedEnd(LearningPeriod latest) {
        if (latest.status() != LearningPeriodStatus.COMPLETED) {
            throw new IllegalStateException("Latest LearningPeriod is neither ACTIVE nor COMPLETED");
        }
        return latest.endCumulativeMinutes();
    }

    private Timeline buildTimeline(
        LearningPeriod active,
        List<AttendedLessonSession> sessions,
        int expectedTotal
    ) {
        int cumulative = 0;
        Instant firstPeriodSessionAt = null;
        Instant thresholdCrossedAt = null;
        boolean boundaryFound = active.startCumulativeMinutes() == 0;

        for (AttendedLessonSession session : sessions) {
            if (cumulative == active.startCumulativeMinutes()) {
                boundaryFound = true;
                if (firstPeriodSessionAt == null) {
                    firstPeriodSessionAt = session.startedAt();
                }
            }
            cumulative = Math.addExact(cumulative, session.durationMinutes());
            if (firstPeriodSessionAt != null
                && thresholdCrossedAt == null
                && cumulative >= active.thresholdMinutes()) {
                thresholdCrossedAt = session.startedAt();
            }
        }

        if (cumulative != expectedTotal
            || expectedTotal < active.startCumulativeMinutes()
            || (!boundaryFound && expectedTotal > active.startCumulativeMinutes())) {
            throw new HistoricalLearningPeriodChangeException(active.studentProgramId());
        }
        return new Timeline(firstPeriodSessionAt, thresholdCrossedAt);
    }

    private record Timeline(Instant firstPeriodSessionAt, Instant thresholdCrossedAt) {
    }
}
