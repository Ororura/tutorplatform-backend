package com.tutorplatform.program.infrastructure.persistence.learningprogram;

import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaLearningProgramRepository implements LearningProgramRepository {

    private final LearningProgramDatabaseRepository databaseRepository;

    JpaLearningProgramRepository(LearningProgramDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public LearningProgramEntity saveAndFlush(LearningProgramEntity learningProgram) {
        return databaseRepository
                .saveAndFlush(new LearningProgramDatabaseModel(learningProgram))
                .toEntity();
    }

    @Override
    public Optional<LearningProgramEntity> findById(UUID learningProgramId) {
        return databaseRepository
                .findById(learningProgramId)
                .map(LearningProgramDatabaseModel::toEntity);
    }

    @Override
    public Optional<LearningProgramEntity> findByIdForUpdate(UUID learningProgramId) {
        return databaseRepository
                .findWithLockById(learningProgramId)
                .map(LearningProgramDatabaseModel::toEntity);
    }
}
