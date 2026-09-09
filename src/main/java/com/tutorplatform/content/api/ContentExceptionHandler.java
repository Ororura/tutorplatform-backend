package com.tutorplatform.content.api;

import com.tutorplatform.content.application.FileTooLargeException;
import com.tutorplatform.content.application.exception.*;
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
public class ContentExceptionHandler {

    @ExceptionHandler(InvalidLessonMaterialException.class)
    ResponseEntity<ApiError> handleInvalidLessonMaterial(InvalidLessonMaterialException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                "VALIDATION_ERROR",
                "Request validation failed",
                Instant.now(),
                MDC.get("traceId"),
                List.of(new ApiErrorDetail(exception.getField(), exception.getMessage()))
        ));
    }

    @ExceptionHandler(TopicNotFoundException.class)
    ResponseEntity<ApiError> handleTopicNotFound(TopicNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.of("TOPIC_NOT_FOUND", "Topic not found", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(LessonMaterialNotFoundException.class)
    ResponseEntity<ApiError> handleLessonMaterialNotFound(LessonMaterialNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.of("LESSON_MATERIAL_NOT_FOUND", "Lesson material not found", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(LessonMaterialPositionConflictException.class)
    ResponseEntity<ApiError> handleLessonMaterialPositionConflict(
            LessonMaterialPositionConflictException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
                "LESSON_MATERIAL_POSITION_CONFLICT",
                "Material position is already used by this topic",
                MDC.get("traceId")
        ));
    }

    @ExceptionHandler(LessonMaterialVersionConflictException.class)
    ResponseEntity<ApiError> handleLessonMaterialVersionConflict(
            LessonMaterialVersionConflictException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
                "LESSON_MATERIAL_VERSION_CONFLICT",
                "Lesson material was modified by another request",
                MDC.get("traceId")
        ));
    }

    @ExceptionHandler(FileTooLargeException.class)
    ResponseEntity<ApiError> handleFileTooLarge(FileTooLargeException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
                ApiError.of("FILE_TOO_LARGE", "File exceeds upload limit", MDC.get("traceId"))
        );
    }
}
