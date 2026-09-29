package com.tutorplatform.content.application.importpackage;

import java.util.List;

public record TaskImport(
        String title,
        String descriptionMarkdown,
        String taskType,
        String difficulty,
        Boolean required,
        ProgrammingConfigImport programmingConfig,
        List<TestCaseImport> testCases) {
    public TaskImport {
        testCases = testCases == null ? List.of() : List.copyOf(testCases);
    }
}
