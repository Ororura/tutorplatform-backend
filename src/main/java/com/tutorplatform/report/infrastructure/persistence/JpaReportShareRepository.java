package com.tutorplatform.report.infrastructure.persistence;

import com.tutorplatform.report.domain.ReportShare;
import com.tutorplatform.report.domain.ReportShareRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaReportShareRepository implements ReportShareRepository {

    private final ReportShareDatabaseRepository databaseRepository;

    JpaReportShareRepository(ReportShareDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public ReportShare saveAndFlush(ReportShare share) {
        return databaseRepository.saveAndFlush(new ReportShareDatabaseModel(share)).toDomain();
    }

    @Override
    public Optional<ReportShare> findByTokenHash(String tokenHash) {
        return databaseRepository.findByTokenHash(tokenHash).map(ReportShareDatabaseModel::toDomain);
    }

    @Override
    public Optional<ReportShare> findByIdAndReportId(UUID shareId, UUID reportId) {
        return databaseRepository.findByIdAndReportId(shareId, reportId)
            .map(ReportShareDatabaseModel::toDomain);
    }

    @Override
    public List<ReportShare> listByReportId(UUID reportId) {
        return databaseRepository.findAllByReportIdOrderByCreatedAtDesc(reportId).stream()
            .map(ReportShareDatabaseModel::toDomain)
            .toList();
    }
}
