package com.tutorplatform.report.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.progress.application.GetCurrentProgressService;
import com.tutorplatform.progress.application.ProgressInterval;
import com.tutorplatform.progress.domain.CurrentProgress;
import com.tutorplatform.report.application.exception.InvalidProgressReportPeriodException;
import com.tutorplatform.report.application.exception.LearningPeriodNotFoundException;
import com.tutorplatform.report.application.exception.ProgressReportConflictException;
import com.tutorplatform.report.domain.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CreateProgressReportDraft {

    private final ProgressReportAuthorization authorization;
    private final LearningPeriodRepository learningPeriodRepository;
    private final ProgressReportRepository progressReportRepository;
    private final GetCurrentProgressService progressService;
    private final ProgressReportSnapshotV1Factory snapshotFactory;
    private final ProgressReportDraftWriter writer;

    CreateProgressReportDraft(
            ProgressReportAuthorization authorization,
            LearningPeriodRepository learningPeriodRepository,
            ProgressReportRepository progressReportRepository,
            GetCurrentProgressService progressService,
            ProgressReportSnapshotV1Factory snapshotFactory,
            ProgressReportDraftWriter writer) {
        this.authorization = authorization;
        this.learningPeriodRepository = learningPeriodRepository;
        this.progressReportRepository = progressReportRepository;
        this.progressService = progressService;
        this.snapshotFactory = snapshotFactory;
        this.writer = writer;
    }

    public ProgressReport create(
            AuthenticatedUser principal,
            UUID studentId,
            UUID studentProgramId,
            UUID learningPeriodId) {
        var authorized = authorization.require(principal, studentId, studentProgramId);
        return create(authorized, studentProgramId, learningPeriodId);
    }

    public ProgressReport create(
            AuthenticatedUser principal, UUID studentProgramId, UUID learningPeriodId) {
        var authorized = authorization.require(principal, studentProgramId);
        return create(authorized, studentProgramId, learningPeriodId);
    }

    private ProgressReport create(
            ProgressReportAuthorization.AuthorizedProgram authorized,
            UUID studentProgramId,
            UUID learningPeriodId) {
        LearningPeriod period =
                learningPeriodRepository
                        .findById(learningPeriodId)
                        .orElseThrow(LearningPeriodNotFoundException::new);
        validatePeriod(period, studentProgramId);
        if (progressReportRepository.existsByLearningPeriodId(learningPeriodId)) {
            throw new ProgressReportConflictException();
        }

        int learningMinutes =
                Math.subtractExact(period.endCumulativeMinutes(), period.startCumulativeMinutes());
        CurrentProgress progress =
                progressService.getProgressSnapshot(
                        studentProgramId,
                        new ProgressInterval(period.startedAt(), period.completedAt()));
        Instant now = Instant.now();
        return writer.save(
                ProgressReport.draft(
                        UUID.randomUUID(),
                        studentProgramId,
                        learningPeriodId,
                        authorized.teacherId(),
                        period.startedAt(),
                        period.completedAt(),
                        learningMinutes,
                        snapshotFactory.create(progress, learningMinutes),
                        now));
    }

    private void validatePeriod(LearningPeriod period, UUID studentProgramId) {
        if (!period.studentProgramId().equals(studentProgramId)) {
            throw new InvalidProgressReportPeriodException(
                    "LearningPeriod does not belong to StudentProgram");
        }
        if (period.status() != LearningPeriodStatus.COMPLETED) {
            throw new InvalidProgressReportPeriodException("LearningPeriod is not COMPLETED");
        }
        if (period.startedAt() == null
                || period.completedAt() == null
                || period.completedAt().isBefore(period.startedAt())) {
            throw new InvalidProgressReportPeriodException(
                    "LearningPeriod has no valid actual time boundaries");
        }
        if (period.endCumulativeMinutes() == null
                || period.endCumulativeMinutes() < period.startCumulativeMinutes()) {
            throw new InvalidProgressReportPeriodException(
                    "LearningPeriod has no valid cumulative boundaries");
        }
    }
}
