package com.tutorplatform.student.api.codeexecution;

import com.tutorplatform.shared.api.ApiError;
import com.tutorplatform.student.application.exception.RunCodeException;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(0)
public class StudentRunCodeExceptionHandler {

    @ExceptionHandler(RunCodeException.class)
    ResponseEntity<ApiError> handleRunCode(RunCodeException exception) {
        return switch (exception.reason()) {
            case TASK_NOT_FOUND -> error(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "Task not found");
            case TASK_NOT_EXECUTABLE -> error(
                HttpStatus.CONFLICT, "TASK_NOT_EXECUTABLE", "Task cannot be executed"
            );
            case TASK_EXECUTION_DISABLED -> error(
                HttpStatus.CONFLICT, "TASK_EXECUTION_DISABLED", "Task execution is disabled"
            );
            case EXECUTION_CONTEXT_INVALID -> error(
                HttpStatus.NOT_FOUND, "EXECUTION_CONTEXT_INVALID", "Execution context is invalid"
            );
        };
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiError.of(code, message, MDC.get("traceId")));
    }
}
