package com.tutorplatform.task.application;

import com.tutorplatform.task.domain.programming.ComparisonMode;

import java.util.UUID;

public record TaskTestCaseInput(
    UUID id,
    String inputText,
    String expectedOutput,
    Boolean hidden,
    ComparisonMode comparisonMode,
    Integer position
) {
}
