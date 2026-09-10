package com.tutorplatform.progress.api;

import com.tutorplatform.progress.application.exception.*;
import com.tutorplatform.shared.api.ApiError;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProgressExceptionHandler {

    @ExceptionHandler(InvalidProgressShareExpirationException.class)
    ResponseEntity<ApiError> handleInvalidShareExpiration(InvalidProgressShareExpirationException exception) {
        return ResponseEntity.badRequest().body(
            ApiError.of("PROGRESS_SHARE_EXPIRATION_INVALID", "Expiration must be in the future", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(ProgressShareNotFoundException.class)
    ResponseEntity<ApiError> handleShareNotFound(ProgressShareNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiError.of("PROGRESS_SHARE_NOT_FOUND", "Progress share not found", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(ProgressShareExpiredException.class)
    ResponseEntity<ApiError> handleShareExpired(ProgressShareExpiredException exception) {
        return gone("PROGRESS_SHARE_EXPIRED", "Progress share has expired");
    }

    @ExceptionHandler(ProgressShareRevokedException.class)
    ResponseEntity<ApiError> handleShareRevoked(ProgressShareRevokedException exception) {
        return gone("PROGRESS_SHARE_REVOKED", "Progress share has been revoked");
    }

    @ExceptionHandler(ProgressStudentProgramNotFoundException.class)
    ResponseEntity<ApiError> handleStudentProgramNotFound(
        ProgressStudentProgramNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiError.of("STUDENT_PROGRAM_NOT_FOUND", "Student program not found", MDC.get("traceId"))
        );
    }

    private ResponseEntity<ApiError> gone(String code, String message) {
        return ResponseEntity.status(HttpStatus.GONE).body(ApiError.of(code, message, MDC.get("traceId")));
    }
}
