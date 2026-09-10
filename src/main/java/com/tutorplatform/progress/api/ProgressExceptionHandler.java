package com.tutorplatform.progress.api;

import com.tutorplatform.progress.application.exception.ProgressStudentProgramNotFoundException;
import com.tutorplatform.shared.api.ApiError;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProgressExceptionHandler {

    @ExceptionHandler(ProgressStudentProgramNotFoundException.class)
    ResponseEntity<ApiError> handleStudentProgramNotFound(
        ProgressStudentProgramNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiError.of("STUDENT_PROGRAM_NOT_FOUND", "Student program not found", MDC.get("traceId"))
        );
    }
}
