package com.tutorplatform.file.api;

import com.tutorplatform.file.application.FileStorageException;
import com.tutorplatform.shared.api.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(0)
public class FileExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(FileExceptionHandler.class);

    @ExceptionHandler(FileStorageException.class)
    ResponseEntity<ApiError> handleStorageFailure(FileStorageException exception) {
        log.error("File storage operation failed", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiError.of("FILE_STORAGE_ERROR", "File storage operation failed", MDC.get("traceId"))
        );
    }
}
