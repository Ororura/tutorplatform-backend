package com.tutorplatform.program.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModuleRepository {
    ModuleEntity saveAndFlush(ModuleEntity module);

    Optional<ModuleEntity> findById(UUID moduleId);

    List<ModuleEntity> findByLearningProgramId(UUID learningProgramId);

    void updatePosition(UUID learningProgramId, UUID moduleId, int position);

    void deleteById(UUID moduleId);

    int findMaxPositionByLearningProgramId(UUID learningProgramId);
}
