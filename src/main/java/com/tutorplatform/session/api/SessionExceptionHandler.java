package com.tutorplatform.session.api;

import com.tutorplatform.session.application.exception.*;
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
public class SessionExceptionHandler {

    @ExceptionHandler(InvalidSessionListParameterException.class)
    ResponseEntity<ApiError> handleInvalidSessionListParameter(InvalidSessionListParameterException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
            "VALIDATION_ERROR",
            "Request validation failed",
            Instant.now(),
            MDC.get("traceId"),
            List.of(new ApiErrorDetail(exception.getField(), exception.getMessage()))
        ));
    }

    @ExceptionHandler(InvalidLessonSessionTopicsException.class)
    ResponseEntity<ApiError> handleInvalidLessonSessionTopics(InvalidLessonSessionTopicsException exception) {
        return ResponseEntity.badRequest().body(
            ApiError.of("LESSON_SESSION_TOPICS_INVALID", exception.getMessage(), MDC.get("traceId"))
        );
    }

    @ExceptionHandler(TopicOutsideStudentProgramException.class)
    ResponseEntity<ApiError> handleTopicOutsideStudentProgram(TopicOutsideStudentProgramException exception) {
        return ResponseEntity.badRequest().body(ApiError.of(
            "LESSON_SESSION_TOPIC_INVALID",
            "Topic does not belong to the student program",
            MDC.get("traceId")
        ));
    }

    @ExceptionHandler(StudentProgramNotFoundException.class)
    ResponseEntity<ApiError> handleStudentProgramNotFound(StudentProgramNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiError.of("STUDENT_PROGRAM_NOT_FOUND", "Student program not found", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(LessonSessionNotFoundException.class)
    ResponseEntity<ApiError> handleLessonSessionNotFound(LessonSessionNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiError.of("LESSON_SESSION_NOT_FOUND", "Lesson session not found", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(LessonSessionVersionConflictException.class)
    ResponseEntity<ApiError> handleLessonSessionVersionConflict(LessonSessionVersionConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
            "LESSON_SESSION_VERSION_CONFLICT",
            "Lesson session was modified by another request",
            MDC.get("traceId")
        ));
    }
}
