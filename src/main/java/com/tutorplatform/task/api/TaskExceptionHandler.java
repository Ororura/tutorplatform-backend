package com.tutorplatform.task.api;

import com.tutorplatform.shared.api.ApiError;
import com.tutorplatform.shared.api.ApiErrorDetail;
import com.tutorplatform.task.application.exception.*;
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
public class TaskExceptionHandler {

    @ExceptionHandler({InvalidTaskException.class, InvalidTaskListParameterException.class})
    ResponseEntity<ApiError> handleInvalidTask(RuntimeException exception) {
        String field = exception instanceof InvalidTaskException invalid
                ? invalid.getField()
                : ((InvalidTaskListParameterException) exception).getField();
        return ResponseEntity.badRequest().body(new ApiError(
                "VALIDATION_ERROR",
                "Request validation failed",
                Instant.now(),
                MDC.get("traceId"),
                List.of(new ApiErrorDetail(field, exception.getMessage()))
        ));
    }

    @ExceptionHandler(TaskNotFoundException.class)
    ResponseEntity<ApiError> handleTaskNotFound(TaskNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.of("TASK_NOT_FOUND", "Task not found", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(TaskSubjectNotFoundException.class)
    ResponseEntity<ApiError> handleTaskSubjectNotFound(TaskSubjectNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.of("SUBJECT_NOT_FOUND", "Subject not found", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(TaskTopicNotFoundException.class)
    ResponseEntity<ApiError> handleTaskTopicNotFound(TaskTopicNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.of("TOPIC_NOT_FOUND", "Topic not found", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(TaskSubjectMismatchException.class)
    ResponseEntity<ApiError> handleTaskSubjectMismatch(TaskSubjectMismatchException exception) {
        return ResponseEntity.badRequest().body(ApiError.of(
                "TASK_SUBJECT_MISMATCH",
                "Task and topic must belong to the same subject",
                MDC.get("traceId")
        ));
    }

    @ExceptionHandler(TaskTopicPositionConflictException.class)
    ResponseEntity<ApiError> handleTaskTopicPositionConflict(TaskTopicPositionConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
                "TASK_TOPIC_POSITION_CONFLICT",
                "Task position is already used by this topic",
                MDC.get("traceId")
        ));
    }

    @ExceptionHandler(TaskAlreadyAttachedException.class)
    ResponseEntity<ApiError> handleTaskAlreadyAttached(TaskAlreadyAttachedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiError.of("TASK_ALREADY_ATTACHED", "Task is already attached to this topic", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(TaskVersionConflictException.class)
    ResponseEntity<ApiError> handleTaskVersionConflict(TaskVersionConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
                "TASK_VERSION_CONFLICT",
                "Task was modified by another request",
                MDC.get("traceId")
        ));
    }
}
