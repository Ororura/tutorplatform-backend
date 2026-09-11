package com.tutorplatform.report.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportShareRepository {

    ReportShare saveAndFlush(ReportShare share);

    Optional<ReportShare> findByTokenHash(String tokenHash);

    Optional<ReportShare> findByIdAndReportId(UUID shareId, UUID reportId);

    List<ReportShare> listByReportId(UUID reportId);
}
