package com.tutorplatform.task.application;

import com.tutorplatform.task.domain.programming.ProgrammingLanguage;

public record ProgrammingTaskConfigInput(
        ProgrammingLanguage language,
        String starterCode,
        Boolean executionEnabled,
        Integer timeLimitMs,
        Integer memoryLimitMb) {}
