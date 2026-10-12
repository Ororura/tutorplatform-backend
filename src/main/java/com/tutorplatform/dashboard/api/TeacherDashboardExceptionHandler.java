package com.tutorplatform.dashboard.api;

import com.tutorplatform.dashboard.application.InvalidTeacherDashboardAttentionParameterException;
import com.tutorplatform.shared.api.ApiError;
import com.tutorplatform.shared.api.ApiErrorDetail;
import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = TeacherDashboardController.class)
public class TeacherDashboardExceptionHandler {

    @ExceptionHandler(InvalidTeacherDashboardAttentionParameterException.class)
    ResponseEntity<ApiError> handleInvalidParameter(
            InvalidTeacherDashboardAttentionParameterException exception) {
        return ResponseEntity.badRequest()
                .body(
                        new ApiError(
                                "VALIDATION_ERROR",
                                "Request validation failed",
                                Instant.now(),
                                MDC.get("traceId"),
                                List.of(
                                        new ApiErrorDetail(
                                                exception.getField(), exception.getMessage()))));
    }
}
