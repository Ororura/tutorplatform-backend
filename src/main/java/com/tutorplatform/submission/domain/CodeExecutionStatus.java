package com.tutorplatform.submission.domain;

public enum CodeExecutionStatus {
    PENDING,
    RUNNING,
    PASSED,
    FAILED,
    TIMEOUT,
    RUNTIME_ERROR,
    SYSTEM_ERROR
}
