package com.tutorplatform.report.application;

import com.tutorplatform.report.domain.ProgressReportStatus;
import java.util.UUID;

public interface ProgressReportReadQuery {

    ProgressReportSummaryPage findOwnedPage(
            UUID teacherId,
            UUID studentProgramId,
            ProgressReportStatus status,
            int page,
            int size,
            String sortField,
            boolean ascending);
}
