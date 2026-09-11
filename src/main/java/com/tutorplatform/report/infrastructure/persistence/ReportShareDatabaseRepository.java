package com.tutorplatform.report.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ReportShareDatabaseRepository extends JpaRepository<ReportShareDatabaseModel, UUID> {

    Optional<ReportShareDatabaseModel> findByTokenHash(String tokenHash);

    Optional<ReportShareDatabaseModel> findByIdAndReportId(UUID id, UUID reportId);

    List<ReportShareDatabaseModel> findAllByReportIdOrderByCreatedAtDesc(UUID reportId);
}
