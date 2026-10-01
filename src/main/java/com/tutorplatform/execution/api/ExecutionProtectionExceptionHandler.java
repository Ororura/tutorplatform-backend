package com.tutorplatform.execution.api;

import com.tutorplatform.execution.application.ExecutionRateLimitExceededException;
import com.tutorplatform.shared.api.ApiError;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExecutionProtectionExceptionHandler {
    @ExceptionHandler(ExecutionRateLimitExceededException.class)
    ResponseEntity<ApiError> handleRateLimit(ExecutionRateLimitExceededException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.retryAfterSeconds()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(
                        ApiError.of(
                                "EXECUTION_RATE_LIMIT_EXCEEDED",
                                exception.getMessage(),
                                MDC.get("traceId")));
    }
}
