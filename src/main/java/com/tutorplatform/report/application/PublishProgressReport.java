package com.tutorplatform.report.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.report.application.exception.ProgressReportNotFoundException;
import com.tutorplatform.report.application.exception.ProgressReportNotPublishableException;
import com.tutorplatform.report.application.exception.ProgressReportVersionConflictException;
import com.tutorplatform.report.domain.ProgressReport;
import com.tutorplatform.report.domain.ProgressReportRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublishProgressReport {

    private final ProgressReportAuthorization authorization;
    private final ProgressReportRepository repository;

    PublishProgressReport(
            ProgressReportAuthorization authorization, ProgressReportRepository repository) {
        this.authorization = authorization;
        this.repository = repository;
    }

    @Transactional
    public ProgressReport publish(
            AuthenticatedUser principal,
            UUID studentId,
            UUID studentProgramId,
            UUID reportId,
            long version) {
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
        var authorized = authorization.require(principal, studentId, studentProgramId);
        return publishOwned(authorized.teacherId(), studentProgramId, reportId, version);
    }

    @Transactional
    public ProgressReport publish(AuthenticatedUser principal, UUID reportId, long version) {
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
        UUID teacherId = authorization.currentTeacherId(principal);
        ProgressReport current =
                repository
                        .findOwnedById(reportId, teacherId)
                        .orElseThrow(ProgressReportNotFoundException::new);
        return publishCurrent(current, version);
    }

    private ProgressReport publishOwned(
            UUID teacherId, UUID studentProgramId, UUID reportId, long version) {
        ProgressReport current =
                repository
                        .findOwnedById(reportId, studentProgramId, teacherId)
                        .orElseThrow(ProgressReportNotFoundException::new);
        return publishCurrent(current, version);
    }

    private ProgressReport publishCurrent(ProgressReport current, long version) {
        try {
            return repository.saveAndFlush(current.publish(version, Instant.now()));
        } catch (IllegalStateException exception) {
            throw new ProgressReportNotPublishableException(exception.getMessage());
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new ProgressReportVersionConflictException(exception);
        }
    }
}
