package com.tutorplatform.report.api;

import com.tutorplatform.report.application.exception.*;
import com.tutorplatform.shared.api.ApiError;
import com.tutorplatform.shared.api.ApiErrorDetail;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@Order(0)
public class ProgressReportExceptionHandler {

    @ExceptionHandler(InvalidProgressReportListParameterException.class)
    ResponseEntity<ApiError> handleInvalidList(InvalidProgressReportListParameterException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
            "VALIDATION_ERROR", "Request validation failed", Instant.now(), MDC.get("traceId"),
            List.of(new ApiErrorDetail(exception.getField(), exception.getMessage()))
        ));
    }

    @ExceptionHandler(ProgressReportNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(ProgressReportNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(
            "PROGRESS_REPORT_NOT_FOUND", "Progress report not found", MDC.get("traceId")
        ));
    }

    @ExceptionHandler(LearningPeriodNotFoundException.class)
    ResponseEntity<ApiError> handleLearningPeriodNotFound(LearningPeriodNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(
            "LEARNING_PERIOD_NOT_FOUND", "Learning period not found", MDC.get("traceId")
        ));
    }

    @ExceptionHandler(ProgressReportConflictException.class)
    ResponseEntity<ApiError> handleAlreadyExists(ProgressReportConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
            "PROGRESS_REPORT_ALREADY_EXISTS",
            "A progress report already exists for this learning period",
            MDC.get("traceId")
        ));
    }

    @ExceptionHandler(InvalidProgressReportPeriodException.class)
    ResponseEntity<ApiError> handleInvalidPeriod(InvalidProgressReportPeriodException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
            "PROGRESS_REPORT_PERIOD_INVALID", exception.getMessage(), MDC.get("traceId")
        ));
    }

    @ExceptionHandler(ProgressReportNotEditableException.class)
    ResponseEntity<ApiError> handleNotEditable(ProgressReportNotEditableException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
            "PROGRESS_REPORT_NOT_EDITABLE", exception.getMessage(), MDC.get("traceId")
        ));
    }

    @ExceptionHandler(ProgressReportNotPublishableException.class)
    ResponseEntity<ApiError> handleNotPublishable(ProgressReportNotPublishableException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
            "PROGRESS_REPORT_NOT_PUBLISHABLE", exception.getMessage(), MDC.get("traceId")
        ));
    }

    @ExceptionHandler(ProgressReportVersionConflictException.class)
    ResponseEntity<ApiError> handleVersionConflict(ProgressReportVersionConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
            "OPTIMISTIC_LOCK_CONFLICT",
            "Progress report was modified by another request",
            MDC.get("traceId")
        ));
    }
}
