package com.tutorplatform.execution.api;

import com.tutorplatform.execution.application.ExecutionRateLimitExceededException;
import com.tutorplatform.execution.application.SourceCodeTooLargeException;
import com.tutorplatform.shared.api.ApiError;
import com.tutorplatform.shared.api.ApiErrorDetail;
import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(0)
public class ExecutionProtectionExceptionHandler {
    @ExceptionHandler(SourceCodeTooLargeException.class)
    ResponseEntity<ApiError> handleSourceCodeSize(SourceCodeTooLargeException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(
                        new ApiError(
                                "SOURCE_CODE_TOO_LARGE",
                                "Source code exceeds the execution input limit",
                                Instant.now(),
                                MDC.get("traceId"),
                                List.of(new ApiErrorDetail("sourceCode", exception.getMessage()))));
    }

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
