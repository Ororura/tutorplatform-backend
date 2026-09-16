package com.tutorplatform.task.api.testcase;

import com.tutorplatform.task.domain.programming.TaskTestCase;

import java.util.List;

public record TaskTestCasesResponse(List<TaskTestCaseResponse> items) {
    public static TaskTestCasesResponse from(List<TaskTestCase> items) {
        return new TaskTestCasesResponse(items.stream().map(TaskTestCaseResponse::from).toList());
    }
}
