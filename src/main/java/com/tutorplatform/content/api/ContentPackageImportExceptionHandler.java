package com.tutorplatform.content.api;

import com.tutorplatform.content.application.importpackage.ContentPackageConfirmationConflictException;
import com.tutorplatform.content.application.importpackage.ContentPackageDigestMismatchException;
import com.tutorplatform.content.application.importpackage.ContentPackageParseException;
import com.tutorplatform.content.application.importpackage.ContentPackageValidationException;
import com.tutorplatform.shared.api.ApiError;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = TeacherContentPackageImportController.class)
@Order(-1)
public class ContentPackageImportExceptionHandler {
    @ExceptionHandler(ContentPackageParseException.class)
    ResponseEntity<ApiError> handleParse(ContentPackageParseException exception) {
        HttpStatus status =
                exception.code() == ContentPackageParseException.Code.FILE_TOO_LARGE
                        ? HttpStatus.PAYLOAD_TOO_LARGE
                        : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(
                        ApiError.of(
                                exception.code().name(),
                                exception.getMessage(),
                                MDC.get("traceId")));
    }

    @ExceptionHandler(ContentPackageValidationException.class)
    ResponseEntity<ApiError> handleValidation(ContentPackageValidationException exception) {
        return ResponseEntity.badRequest()
                .body(
                        ApiError.of(
                                "VALIDATION_ERROR",
                                "Package validation failed",
                                MDC.get("traceId")));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> handleInvalidParameter(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(ApiError.of("VALIDATION_ERROR", exception.getMessage(), MDC.get("traceId")));
    }

    @ExceptionHandler(ContentPackageDigestMismatchException.class)
    ResponseEntity<ApiError> handleDigestMismatch(ContentPackageDigestMismatchException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of("DIGEST_MISMATCH", exception.getMessage(), MDC.get("traceId")));
    }

    @ExceptionHandler(ContentPackageConfirmationConflictException.class)
    ResponseEntity<ApiError> handleConfirmationConflict(
            ContentPackageConfirmationConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                        ApiError.of(
                                "CONFIRMATION_CONFLICT",
                                exception.getMessage(),
                                MDC.get("traceId")));
    }
}
