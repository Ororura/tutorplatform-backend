package com.tutorplatform.report.domain;

import java.util.Optional;
import java.util.UUID;

public interface ProgressReportRepository {
    ProgressReport saveAndFlush(ProgressReport report);

    Optional<ProgressReport> findById(UUID id);

    Optional<ProgressReport> findByLearningPeriodId(UUID learningPeriodId);

    boolean existsByLearningPeriodId(UUID learningPeriodId);

    ProgressReportPage listByStudentProgramId(UUID studentProgramId, int page, int size);

    Optional<ProgressReport> findOwnedById(UUID id, UUID studentProgramId, UUID teacherId);

    Optional<ProgressReport> findOwnedById(UUID id, UUID teacherId);
}
