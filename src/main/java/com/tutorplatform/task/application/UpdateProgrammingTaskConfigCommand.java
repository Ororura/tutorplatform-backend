package com.tutorplatform.task.application;

public record UpdateProgrammingTaskConfigCommand(
        String starterCode, Boolean executionEnabled, Integer timeLimitMs, Integer memoryLimitMb) {}
