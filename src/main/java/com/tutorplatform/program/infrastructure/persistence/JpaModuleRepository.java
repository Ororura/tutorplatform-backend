package com.tutorplatform.program.infrastructure.persistence;

import com.tutorplatform.program.domain.ModuleEntity;
import com.tutorplatform.program.domain.ModuleRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaModuleRepository implements ModuleRepository {

    private final ModuleDatabaseRepository databaseRepository;

    JpaModuleRepository(ModuleDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public ModuleEntity saveAndFlush(ModuleEntity module) {
        ModuleDatabaseModel model =
                databaseRepository
                        .findById(module.id())
                        .orElseGet(() -> new ModuleDatabaseModel(module));
        model.updateFrom(module);
        return databaseRepository.saveAndFlush(model).toEntity();
    }

    @Override
    public Optional<ModuleEntity> findById(UUID moduleId) {
        return databaseRepository.findById(moduleId).map(ModuleDatabaseModel::toEntity);
    }

    @Override
    public List<ModuleEntity> findByLearningProgramId(UUID learningProgramId) {
        return databaseRepository.findByLearningProgramId(learningProgramId).stream()
                .map(ModuleDatabaseModel::toEntity)
                .toList();
    }

    @Override
    public void updatePosition(UUID learningProgramId, UUID moduleId, int position) {
        if (databaseRepository.updatePosition(learningProgramId, moduleId, position) != 1) {
            throw new IllegalStateException("Learning program module position update failed");
        }
    }

    @Override
    public void deleteById(UUID moduleId) {
        databaseRepository.deleteById(moduleId);
    }

    @Override
    public int findMaxPositionByLearningProgramId(UUID learningProgramId) {
        return databaseRepository.findMaxPositionByLearningProgramId(learningProgramId);
    }
}
