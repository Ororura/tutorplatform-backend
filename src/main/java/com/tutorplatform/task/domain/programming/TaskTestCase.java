package com.tutorplatform.task.domain.programming;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TaskTestCase(
        UUID id,
        UUID taskId,
        String inputText,
        String expectedOutput,
        boolean hidden,
        ComparisonMode comparisonMode,
        int position,
        Instant createdAt) {
    public TaskTestCase {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(taskId, "taskId is required");
        Objects.requireNonNull(expectedOutput, "expectedOutput is required");
        Objects.requireNonNull(comparisonMode, "comparisonMode is required");
        if (position < 0) {
            throw new IllegalArgumentException("position must be greater than or equal to 0");
        }
    }

    public TaskTestCase(
            UUID id,
            UUID taskId,
            String inputText,
            String expectedOutput,
            boolean hidden,
            ComparisonMode comparisonMode,
            int position) {
        this(id, taskId, inputText, expectedOutput, hidden, comparisonMode, position, null);
    }
}
