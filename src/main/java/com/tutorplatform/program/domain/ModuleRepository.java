package com.tutorplatform.program.domain;

import java.util.Optional;
import java.util.UUID;

public interface ModuleRepository {
    ModuleEntity saveAndFlush(ModuleEntity module);

    Optional<ModuleEntity> findById(UUID moduleId);

    int findMaxPositionByLearningProgramId(UUID learningProgramId);
}
