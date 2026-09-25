package com.tutorplatform.report.application;

import com.tutorplatform.report.domain.ProgressReportStatus;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface ProgressReportReadQuery {

    Map<UUID, UUID> findIdsByLearningPeriodIds(Collection<UUID> learningPeriodIds);

    ProgressReportSummaryPage findOwnedPage(
            UUID teacherId,
            UUID studentProgramId,
            ProgressReportStatus status,
            int page,
            int size,
            String sortField,
            boolean ascending);
}
