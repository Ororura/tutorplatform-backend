package com.tutorplatform.program.domain.learningprogram;

import java.util.Optional;
import java.util.UUID;

public interface LearningProgramRepository {
    LearningProgramEntity saveAndFlush(LearningProgramEntity learningProgram);
    Optional<LearningProgramEntity> findById(UUID learningProgramId);
}
