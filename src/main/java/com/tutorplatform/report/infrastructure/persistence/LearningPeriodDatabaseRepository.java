package com.tutorplatform.report.infrastructure.persistence;

import com.tutorplatform.report.domain.LearningPeriodStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

interface LearningPeriodDatabaseRepository
        extends JpaRepository<LearningPeriodDatabaseModel, UUID> {

    Optional<LearningPeriodDatabaseModel> findByStudentProgramIdAndStatus(
            UUID studentProgramId, LearningPeriodStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LearningPeriodDatabaseModel> findWithLockByStudentProgramIdAndStatus(
            UUID studentProgramId, LearningPeriodStatus status);

    Optional<LearningPeriodDatabaseModel> findTopByStudentProgramIdOrderBySequenceNoDesc(
            UUID studentProgramId);

    List<LearningPeriodDatabaseModel> findAllByStudentProgramIdOrderBySequenceNo(
            UUID studentProgramId);
}
