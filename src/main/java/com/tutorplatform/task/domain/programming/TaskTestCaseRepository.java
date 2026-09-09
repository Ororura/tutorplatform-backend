package com.tutorplatform.task.domain.programming;

import java.util.List;
import java.util.UUID;

public interface TaskTestCaseRepository {
    List<TaskTestCase> saveAllAndFlush(List<TaskTestCase> testCases);
    List<TaskTestCase> findAllByTaskId(UUID taskId);
    void deleteAllByTaskIdAndFlush(UUID taskId);
}
