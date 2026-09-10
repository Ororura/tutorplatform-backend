package com.tutorplatform.assessment.api;

import com.tutorplatform.assessment.application.exception.InvalidTeacherAssessmentScoreException;
import com.tutorplatform.assessment.application.exception.TeacherAssessmentConflictException;
import com.tutorplatform.assessment.application.exception.TeacherAssessmentNotFoundException;
import com.tutorplatform.shared.api.ApiError;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(0)
public class AssessmentExceptionHandler {

    @ExceptionHandler(TeacherAssessmentNotFoundException.class)
    ResponseEntity<ApiError> handleAssessmentNotFound(TeacherAssessmentNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiError.of("ASSESSMENT_NOT_FOUND", "Assessment not found", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(InvalidTeacherAssessmentScoreException.class)
    ResponseEntity<ApiError> handleInvalidScore(InvalidTeacherAssessmentScoreException exception) {
        return ResponseEntity.badRequest().body(
            ApiError.of("ASSESSMENT_SCORE_INVALID", exception.getMessage(), MDC.get("traceId"))
        );
    }

    @ExceptionHandler(TeacherAssessmentConflictException.class)
    ResponseEntity<ApiError> handleAssessmentConflict(TeacherAssessmentConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
            "ASSESSMENT_CONFLICT",
            "Assessment was modified by another request",
            MDC.get("traceId")
        ));
    }
}
