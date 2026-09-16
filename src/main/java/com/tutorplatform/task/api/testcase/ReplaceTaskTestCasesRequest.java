package com.tutorplatform.task.api.testcase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReplaceTaskTestCasesRequest(
    @NotEmpty List<@Valid TaskTestCaseRequest> items
) {
}
