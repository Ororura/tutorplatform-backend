package com.tutorplatform.report.api;

import com.tutorplatform.report.application.exception.*;
import com.tutorplatform.report.domain.UnsupportedProgressReportSnapshotException;
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

    @ExceptionHandler(ProgressReportPdfNotAvailableException.class)
    ResponseEntity<ApiError> handlePdfNotAvailable(ProgressReportPdfNotAvailableException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
            "PROGRESS_REPORT_PDF_NOT_AVAILABLE", exception.getMessage(), MDC.get("traceId")
        ));
    }

    @ExceptionHandler(ProgressReportPdfRenderException.class)
    ResponseEntity<ApiError> handlePdfRenderFailure(ProgressReportPdfRenderException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.of(
            "PROGRESS_REPORT_PDF_FAILED", "Progress report PDF generation failed", MDC.get("traceId")
        ));
    }

    @ExceptionHandler(UnsupportedProgressReportSnapshotException.class)
    ResponseEntity<ApiError> handleUnsupportedSnapshot(UnsupportedProgressReportSnapshotException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.of(
            "PROGRESS_REPORT_SNAPSHOT_UNSUPPORTED",
            "Progress report snapshot version is not supported",
            MDC.get("traceId")
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

    @ExceptionHandler(InvalidReportShareExpirationException.class)
    ResponseEntity<ApiError> handleInvalidShareExpiration(InvalidReportShareExpirationException exception) {
        return ResponseEntity.badRequest().body(ApiError.of(
            "REPORT_SHARE_EXPIRATION_INVALID", "Expiration must be in the future", MDC.get("traceId")
        ));
    }

    @ExceptionHandler(ReportShareNotAllowedException.class)
    ResponseEntity<ApiError> handleShareNotAllowed(ReportShareNotAllowedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
            "REPORT_SHARE_NOT_ALLOWED", "Only published progress reports can be shared", MDC.get("traceId")
        ));
    }

    @ExceptionHandler(ReportShareNotFoundException.class)
    ResponseEntity<ApiError> handleShareNotFound(ReportShareNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(
            "REPORT_SHARE_NOT_FOUND", "Report share not found", MDC.get("traceId")
        ));
    }

    @ExceptionHandler(ReportShareExpiredException.class)
    ResponseEntity<ApiError> handleShareExpired(ReportShareExpiredException exception) {
        return gone("REPORT_SHARE_EXPIRED", "Report share has expired");
    }

    @ExceptionHandler(ReportShareRevokedException.class)
    ResponseEntity<ApiError> handleShareRevoked(ReportShareRevokedException exception) {
        return gone("REPORT_SHARE_REVOKED", "Report share has been revoked");
    }

    private ResponseEntity<ApiError> gone(String code, String message) {
        return ResponseEntity.status(HttpStatus.GONE).body(ApiError.of(code, message, MDC.get("traceId")));
    }
}
