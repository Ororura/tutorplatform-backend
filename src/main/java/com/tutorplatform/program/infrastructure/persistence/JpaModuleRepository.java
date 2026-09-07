package com.tutorplatform.program.infrastructure.persistence;

import com.tutorplatform.program.domain.ModuleEntity;
import com.tutorplatform.program.domain.ModuleRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaModuleRepository implements ModuleRepository {

    private final ModuleDatabaseRepository databaseRepository;

    public JpaModuleRepository(ModuleDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public ModuleEntity saveAndFlush(ModuleEntity module) {
        ModuleDatabaseModel model = databaseRepository.findById(module.getId())
                .orElseGet(() -> new ModuleDatabaseModel(module));
        model.updateFrom(module);
        return databaseRepository.saveAndFlush(model).toEntity();
    }

    @Override
    public Optional<ModuleEntity> findById(UUID moduleId) {
        return databaseRepository.findById(moduleId).map(ModuleDatabaseModel::toEntity);
    }
}
