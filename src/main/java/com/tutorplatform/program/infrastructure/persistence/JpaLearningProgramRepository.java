package com.tutorplatform.program.infrastructure.persistence;

import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaLearningProgramRepository implements LearningProgramRepository {

    private final LearningProgramDatabaseRepository databaseRepository;

    public JpaLearningProgramRepository(LearningProgramDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public LearningProgramEntity saveAndFlush(LearningProgramEntity learningProgram) {
        return databaseRepository.saveAndFlush(new LearningProgramDatabaseModel(learningProgram)).toEntity();
    }

    @Override
    public Optional<LearningProgramEntity> findById(UUID learningProgramId) {
        return databaseRepository.findById(learningProgramId).map(LearningProgramDatabaseModel::toEntity);
    }
}
