package com.tutorplatform.task.api.request;

import com.tutorplatform.task.domain.programming.ComparisonMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record TaskTestCaseRequest(
    UUID id,
    String inputText,
    @NotNull String expectedOutput,
    @NotNull Boolean hidden,
    @NotNull ComparisonMode comparisonMode,
    @NotNull @PositiveOrZero Integer position
) {
}
