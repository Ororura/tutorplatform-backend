package com.tutorplatform.report.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.report.application.exception.ProgressReportNotEditableException;
import com.tutorplatform.report.application.exception.ProgressReportNotFoundException;
import com.tutorplatform.report.application.exception.ProgressReportVersionConflictException;
import com.tutorplatform.report.domain.ProgressReport;
import com.tutorplatform.report.domain.ProgressReportRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EditProgressReportDraft {

    private final ProgressReportAuthorization authorization;
    private final ProgressReportRepository repository;

    EditProgressReportDraft(
            ProgressReportAuthorization authorization, ProgressReportRepository repository) {
        this.authorization = authorization;
        this.repository = repository;
    }

    @Transactional
    public ProgressReport edit(
            AuthenticatedUser principal,
            UUID studentId,
            UUID studentProgramId,
            UUID reportId,
            EditProgressReportDraftCommand command) {
        var authorized = authorization.require(principal, studentId, studentProgramId);
        return editOwned(authorized.teacherId(), studentProgramId, reportId, command);
    }

    @Transactional
    public ProgressReport edit(
            AuthenticatedUser principal, UUID reportId, EditProgressReportDraftCommand command) {
        UUID teacherId = authorization.currentTeacherId(principal);
        ProgressReport current =
                repository
                        .findOwnedById(reportId, teacherId)
                        .orElseThrow(ProgressReportNotFoundException::new);
        return editCurrent(current, command);
    }

    private ProgressReport editOwned(
            UUID teacherId,
            UUID studentProgramId,
            UUID reportId,
            EditProgressReportDraftCommand command) {
        ProgressReport current =
                repository
                        .findOwnedById(reportId, studentProgramId, teacherId)
                        .orElseThrow(ProgressReportNotFoundException::new);
        return editCurrent(current, command);
    }

    private ProgressReport editCurrent(
            ProgressReport current, EditProgressReportDraftCommand command) {
        try {
            ProgressReport edited =
                    current.editDraft(
                            command.teacherSummaryPresent()
                                    ? command.teacherSummary()
                                    : current.teacherSummary(),
                            command.nextPeriodPlanPresent()
                                    ? command.nextPeriodPlan()
                                    : current.nextPeriodPlan(),
                            command.version(),
                            Instant.now());
            return repository.saveAndFlush(edited);
        } catch (IllegalStateException exception) {
            throw new ProgressReportNotEditableException(exception.getMessage());
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new ProgressReportVersionConflictException(exception);
        }
    }
}
