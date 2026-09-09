package com.tutorplatform.task.infrastructure.persistence.programming;

import com.tutorplatform.task.domain.programming.TaskTestCase;
import com.tutorplatform.task.domain.programming.TaskTestCaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JpaTaskTestCaseRepository implements TaskTestCaseRepository {
    private final TaskTestCaseDatabaseRepository repository;

    JpaTaskTestCaseRepository(TaskTestCaseDatabaseRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<TaskTestCase> saveAllAndFlush(List<TaskTestCase> testCases) {
        return repository.saveAllAndFlush(testCases.stream().map(TaskTestCaseDatabaseModel::new).toList())
            .stream().map(TaskTestCaseDatabaseModel::toDomain).toList();
    }

    @Override
    public List<TaskTestCase> findAllByTaskId(UUID taskId) {
        return repository.findAllByTaskIdOrderByPosition(taskId).stream()
            .map(TaskTestCaseDatabaseModel::toDomain).toList();
    }

    @Override
    public void deleteAllByTaskIdAndFlush(UUID taskId) {
        repository.deleteAllByTaskId(taskId);
        repository.flush();
    }
}
