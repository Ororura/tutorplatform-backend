package com.tutorplatform.execution.application;

import java.util.Objects;
import java.util.UUID;

public record ExecutionTestCase(
    UUID id,
    String inputText,
    String expectedOutput,
    ExecutionComparisonMode comparisonMode
) {
    public ExecutionTestCase {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(expectedOutput, "expectedOutput must not be null");
        Objects.requireNonNull(comparisonMode, "comparisonMode must not be null");
    }
}
