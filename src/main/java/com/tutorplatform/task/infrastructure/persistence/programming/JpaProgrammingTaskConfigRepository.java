package com.tutorplatform.task.infrastructure.persistence.programming;

import com.tutorplatform.task.domain.programming.ProgrammingTaskConfig;
import com.tutorplatform.task.domain.programming.ProgrammingTaskConfigRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaProgrammingTaskConfigRepository implements ProgrammingTaskConfigRepository {
    private final ProgrammingTaskConfigDatabaseRepository repository;

    JpaProgrammingTaskConfigRepository(ProgrammingTaskConfigDatabaseRepository repository) {
        this.repository = repository;
    }

    @Override
    public ProgrammingTaskConfig saveAndFlush(ProgrammingTaskConfig config) {
        return repository.saveAndFlush(new ProgrammingTaskConfigDatabaseModel(config)).toDomain();
    }

    @Override
    public Optional<ProgrammingTaskConfig> findByTaskId(UUID taskId) {
        return repository.findById(taskId).map(ProgrammingTaskConfigDatabaseModel::toDomain);
    }
}
