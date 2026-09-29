package com.tutorplatform.content.application.importpackage;

public record ProgrammingConfigImport(
        String language,
        String starterCode,
        Boolean executionEnabled,
        Integer timeLimitMs,
        Integer memoryLimitMb) {}
