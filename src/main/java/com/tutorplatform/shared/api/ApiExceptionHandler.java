package com.tutorplatform.shared.api;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        List<ApiErrorDetail> details = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new ApiErrorDetail(error.getField(), error.getDefaultMessage()))
            .toList();

        return ResponseEntity.badRequest().body(new ApiError(
            "VALIDATION_ERROR",
            "Request validation failed",
            Instant.now(),
            MDC.get("traceId"),
            details
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(
            ApiError.of("VALIDATION_ERROR", "Request body has an invalid value", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception) {
        List<ApiErrorDetail> details = exception.getConstraintViolations().stream()
            .map(violation -> new ApiErrorDetail(
                violation.getPropertyPath().toString(),
                violation.getMessage()
            ))
            .toList();

        return ResponseEntity.badRequest().body(new ApiError(
            "VALIDATION_ERROR",
            "Request validation failed",
            Instant.now(),
            MDC.get("traceId"),
            details
        ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
            "VALIDATION_ERROR",
            "Request validation failed",
            Instant.now(),
            MDC.get("traceId"),
            List.of(new ApiErrorDetail(exception.getName(), "has an invalid value"))
        ));
    }

    @ExceptionHandler({MissingServletRequestPartException.class,
        MissingServletRequestParameterException.class})
    ResponseEntity<ApiError> handleMissingUploadParameter(Exception exception) {
        return ResponseEntity.badRequest().body(
            ApiError.of(
                "VALIDATION_ERROR",
                "Required request part or parameter is missing",
                MDC.get("traceId")
            )
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
            ApiError.of("FILE_TOO_LARGE", "File exceeds upload limit", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        log.error(
            "Unhandled API exception: traceId={}, failureType={}",
            MDC.get("traceId"), exception.getClass().getName()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiError.of("INTERNAL_ERROR", "Unexpected server failure", MDC.get("traceId"))
        );
    }
}
