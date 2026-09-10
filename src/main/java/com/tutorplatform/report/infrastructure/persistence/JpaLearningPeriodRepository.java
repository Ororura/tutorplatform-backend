package com.tutorplatform.report.infrastructure.persistence;

import com.tutorplatform.report.domain.LearningPeriod;
import com.tutorplatform.report.domain.LearningPeriodRepository;
import com.tutorplatform.report.domain.LearningPeriodStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaLearningPeriodRepository implements LearningPeriodRepository {

    private final LearningPeriodDatabaseRepository databaseRepository;

    JpaLearningPeriodRepository(LearningPeriodDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public LearningPeriod saveAndFlush(LearningPeriod period) {
        return databaseRepository.saveAndFlush(new LearningPeriodDatabaseModel(period)).toDomain();
    }

    @Override
    public Optional<LearningPeriod> findById(UUID id) {
        return databaseRepository.findById(id).map(LearningPeriodDatabaseModel::toDomain);
    }

    @Override
    public Optional<LearningPeriod> findActiveByStudentProgramId(UUID studentProgramId) {
        return databaseRepository.findByStudentProgramIdAndStatus(
            studentProgramId, LearningPeriodStatus.ACTIVE
        ).map(LearningPeriodDatabaseModel::toDomain);
    }

    @Override
    public Optional<LearningPeriod> findActiveByStudentProgramIdForUpdate(UUID studentProgramId) {
        return databaseRepository.findWithLockByStudentProgramIdAndStatus(
            studentProgramId, LearningPeriodStatus.ACTIVE
        ).map(LearningPeriodDatabaseModel::toDomain);
    }

    @Override
    public Optional<LearningPeriod> findLatestByStudentProgramId(UUID studentProgramId) {
        return databaseRepository.findTopByStudentProgramIdOrderBySequenceNoDesc(studentProgramId)
            .map(LearningPeriodDatabaseModel::toDomain);
    }

    @Override
    public List<LearningPeriod> findAllByStudentProgramIdOrderBySequenceNo(UUID studentProgramId) {
        return databaseRepository.findAllByStudentProgramIdOrderBySequenceNo(studentProgramId).stream()
            .map(LearningPeriodDatabaseModel::toDomain)
            .toList();
    }
}
