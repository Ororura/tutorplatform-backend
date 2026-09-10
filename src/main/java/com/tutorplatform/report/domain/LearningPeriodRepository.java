package com.tutorplatform.report.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearningPeriodRepository {
    LearningPeriod saveAndFlush(LearningPeriod period);

    Optional<LearningPeriod> findById(UUID id);

    Optional<LearningPeriod> findActiveByStudentProgramId(UUID studentProgramId);

    Optional<LearningPeriod> findActiveByStudentProgramIdForUpdate(UUID studentProgramId);

    Optional<LearningPeriod> findLatestByStudentProgramId(UUID studentProgramId);

    List<LearningPeriod> findAllByStudentProgramIdOrderBySequenceNo(UUID studentProgramId);
}
