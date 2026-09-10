package com.tutorplatform.homework.api;

import com.tutorplatform.homework.application.exception.*;
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
public class HomeworkExceptionHandler {

    @ExceptionHandler(InvalidHomeworkException.class)
    ResponseEntity<ApiError> handleInvalidHomework(InvalidHomeworkException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
            "VALIDATION_ERROR",
            "Request validation failed",
            Instant.now(),
            MDC.get("traceId"),
            List.of(new ApiErrorDetail(exception.getField(), exception.getMessage()))
        ));
    }

    @ExceptionHandler(HomeworkNotFoundException.class)
    ResponseEntity<ApiError> handleHomeworkNotFound(HomeworkNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiError.of("HOMEWORK_NOT_FOUND", "Homework not found", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(HomeworkStudentProgramNotFoundException.class)
    ResponseEntity<ApiError> handleHomeworkStudentProgramNotFound(HomeworkStudentProgramNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiError.of("STUDENT_PROGRAM_NOT_FOUND", "Student program not found", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(HomeworkTaskSubjectMismatchException.class)
    ResponseEntity<ApiError> handleHomeworkTaskSubjectMismatch(HomeworkTaskSubjectMismatchException exception) {
        return ResponseEntity.badRequest().body(ApiError.of(
            "HOMEWORK_TASK_SUBJECT_MISMATCH",
            "Homework task must belong to the student program subject",
            MDC.get("traceId")
        ));
    }

    @ExceptionHandler(HomeworkTaskNotAssignableException.class)
    ResponseEntity<ApiError> handleHomeworkTaskNotAssignable(HomeworkTaskNotAssignableException exception) {
        return ResponseEntity.badRequest().body(ApiError.of(
            "HOMEWORK_TASK_NOT_ASSIGNABLE",
            "Only active tasks can be assigned to homework",
            MDC.get("traceId")
        ));
    }

    @ExceptionHandler(HomeworkItemPositionConflictException.class)
    ResponseEntity<ApiError> handleHomeworkItemPositionConflict(HomeworkItemPositionConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
            "HOMEWORK_ITEM_POSITION_CONFLICT",
            "Homework item position is already used",
            MDC.get("traceId")
        ));
    }

    @ExceptionHandler(HomeworkVersionConflictException.class)
    ResponseEntity<ApiError> handleHomeworkVersionConflict(HomeworkVersionConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
            "HOMEWORK_VERSION_CONFLICT",
            "Homework was modified by another request",
            MDC.get("traceId")
        ));
    }
}
