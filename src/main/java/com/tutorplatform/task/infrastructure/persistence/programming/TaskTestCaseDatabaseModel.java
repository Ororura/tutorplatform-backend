package com.tutorplatform.task.infrastructure.persistence.programming;

import com.tutorplatform.task.domain.programming.ComparisonMode;
import com.tutorplatform.task.domain.programming.TaskTestCase;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "task_test_cases")
class TaskTestCaseDatabaseModel {
    @Id private UUID id;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "input_text")
    private String inputText;

    @Column(name = "expected_output", nullable = false)
    private String expectedOutput;

    @Column(nullable = false)
    private boolean hidden;

    @Enumerated(EnumType.STRING)
    @Column(name = "comparison_mode", nullable = false, length = 32)
    private ComparisonMode comparisonMode;

    @Column(nullable = false)
    private int position;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TaskTestCaseDatabaseModel() {}

    TaskTestCaseDatabaseModel(TaskTestCase testCase) {
        id = testCase.id();
        taskId = testCase.taskId();
        inputText = testCase.inputText();
        expectedOutput = testCase.expectedOutput();
        hidden = testCase.hidden();
        comparisonMode = testCase.comparisonMode();
        position = testCase.position();
        createdAt = testCase.createdAt();
    }

    TaskTestCase toDomain() {
        return new TaskTestCase(
                id, taskId, inputText, expectedOutput, hidden, comparisonMode, position, createdAt);
    }
}
