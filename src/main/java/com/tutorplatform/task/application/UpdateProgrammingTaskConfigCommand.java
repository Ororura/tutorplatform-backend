package com.tutorplatform.task.application;

import com.tutorplatform.task.domain.programming.ProgrammingLanguage;

public record UpdateProgrammingTaskConfigCommand(
        ProgrammingLanguage language,
        String starterCode,
        Boolean executionEnabled,
        Integer timeLimitMs,
        Integer memoryLimitMb) {
    public UpdateProgrammingTaskConfigCommand(
            String starterCode,
            Boolean executionEnabled,
            Integer timeLimitMs,
            Integer memoryLimitMb) {
        this(null, starterCode, executionEnabled, timeLimitMs, memoryLimitMb);
    }
}
