package com.tutorplatform.student.api.coderunner;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ExecutionStatus", description = "Outcome of isolated code execution", enumAsRef = true)
public enum RunCodeExecutionStatus {
    PASSED,
    FAILED,
    TIMEOUT,
    RUNTIME_ERROR,
    SYSTEM_ERROR
}
