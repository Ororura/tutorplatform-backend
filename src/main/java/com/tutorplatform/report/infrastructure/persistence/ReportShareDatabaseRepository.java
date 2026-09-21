package com.tutorplatform.report.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReportShareDatabaseRepository extends JpaRepository<ReportShareDatabaseModel, UUID> {

    Optional<ReportShareDatabaseModel> findByTokenHash(String tokenHash);

    Optional<ReportShareDatabaseModel> findByIdAndReportId(UUID id, UUID reportId);

    List<ReportShareDatabaseModel> findAllByReportIdOrderByCreatedAtDesc(UUID reportId);
}
