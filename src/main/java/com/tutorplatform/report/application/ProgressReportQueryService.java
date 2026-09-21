package com.tutorplatform.report.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.report.application.exception.InvalidProgressReportListParameterException;
import com.tutorplatform.report.application.exception.ProgressReportNotFoundException;
import com.tutorplatform.report.domain.ProgressReport;
import com.tutorplatform.report.domain.ProgressReportPage;
import com.tutorplatform.report.domain.ProgressReportRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProgressReportQueryService {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of(
                    "createdAt",
                    "updatedAt",
                    "periodStartedAt",
                    "periodEndedAt",
                    "publishedAt",
                    "status");

    private final ProgressReportAuthorization authorization;
    private final ProgressReportRepository repository;
    private final ProgressReportReadQuery readQuery;

    ProgressReportQueryService(
            ProgressReportAuthorization authorization,
            ProgressReportRepository repository,
            ProgressReportReadQuery readQuery) {
        this.authorization = authorization;
        this.repository = repository;
        this.readQuery = readQuery;
    }

    public ProgressReport get(AuthenticatedUser principal, UUID reportId) {
        UUID teacherId = authorization.currentTeacherId(principal);
        return repository
                .findOwnedById(reportId, teacherId)
                .orElseThrow(ProgressReportNotFoundException::new);
    }

    public ProgressReport get(
            AuthenticatedUser principal, UUID studentId, UUID studentProgramId, UUID reportId) {
        var authorized = authorization.require(principal, studentId, studentProgramId);
        return repository
                .findOwnedById(reportId, studentProgramId, authorized.teacherId())
                .orElseThrow(ProgressReportNotFoundException::new);
    }

    public ProgressReportPageResult list(
            AuthenticatedUser principal,
            UUID studentId,
            UUID studentProgramId,
            int page,
            int size) {
        authorization.require(principal, studentId, studentProgramId);
        ProgressReportPage result = repository.listByStudentProgramId(studentProgramId, page, size);
        return new ProgressReportPageResult(
                result.items(), page, size, result.totalElements(), result.totalPages());
    }

    public ProgressReportSummaryPageResult list(
            AuthenticatedUser principal,
            UUID studentProgramId,
            com.tutorplatform.report.domain.ProgressReportStatus status,
            int page,
            int size,
            String sort) {
        SortParameters parameters = validateListParameters(page, size, sort);
        UUID teacherId = authorization.currentTeacherId(principal);
        ProgressReportSummaryPage result =
                readQuery.findOwnedPage(
                        teacherId,
                        studentProgramId,
                        status,
                        page,
                        size,
                        parameters.field(),
                        parameters.ascending());
        return new ProgressReportSummaryPageResult(
                result.items(), page, size, result.totalElements(), result.totalPages());
    }

    private SortParameters validateListParameters(int page, int size, String sort) {
        if (page < 0) {
            throw new InvalidProgressReportListParameterException(
                    "page", "must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new InvalidProgressReportListParameterException(
                    "size", "must be between 1 and 100");
        }
        String[] parts = sort.split(",", -1);
        if (parts.length != 2 || !ALLOWED_SORT_FIELDS.contains(parts[0])) {
            throw new InvalidProgressReportListParameterException(
                    "sort", "uses an unsupported field");
        }
        if (!parts[1].equals("asc") && !parts[1].equals("desc")) {
            throw new InvalidProgressReportListParameterException(
                    "sort", "direction must be asc or desc");
        }
        return new SortParameters(parts[0], parts[1].equals("asc"));
    }

    private record SortParameters(String field, boolean ascending) {}
}
