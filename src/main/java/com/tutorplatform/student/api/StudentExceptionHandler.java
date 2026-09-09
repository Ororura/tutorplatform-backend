package com.tutorplatform.student.api;

import com.tutorplatform.shared.api.ApiError;
import com.tutorplatform.shared.api.ApiErrorDetail;
import com.tutorplatform.student.application.exception.InvalidStudentListParameterException;
import com.tutorplatform.student.application.exception.PublicStudentInviteAlreadyAcceptedException;
import com.tutorplatform.student.application.exception.StudentAlreadyRegisteredException;
import com.tutorplatform.student.application.exception.StudentInviteAlreadyAcceptedException;
import com.tutorplatform.student.application.exception.StudentInviteEmailConflictException;
import com.tutorplatform.student.application.exception.StudentInviteExpiredException;
import com.tutorplatform.student.application.exception.StudentInviteNotFoundException;
import com.tutorplatform.student.application.exception.StudentInviteRevokedException;
import com.tutorplatform.student.application.exception.StudentNotFoundException;
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
public class StudentExceptionHandler {

    @ExceptionHandler(InvalidStudentListParameterException.class)
    ResponseEntity<ApiError> handleInvalidStudentListParameter(InvalidStudentListParameterException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                "VALIDATION_ERROR",
                "Request validation failed",
                Instant.now(),
                MDC.get("traceId"),
                List.of(new ApiErrorDetail(exception.getField(), exception.getMessage()))
        ));
    }

    @ExceptionHandler(StudentNotFoundException.class)
    ResponseEntity<ApiError> handleStudentNotFound(StudentNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.of("STUDENT_NOT_FOUND", "Student not found", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(StudentAlreadyRegisteredException.class)
    ResponseEntity<ApiError> handleStudentAlreadyRegistered(StudentAlreadyRegisteredException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiError.of("STUDENT_ALREADY_REGISTERED", "Student is already registered", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(StudentInviteNotFoundException.class)
    ResponseEntity<ApiError> handleStudentInviteNotFound(StudentInviteNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.of("STUDENT_INVITE_NOT_FOUND", "Student invitation not found", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(StudentInviteAlreadyAcceptedException.class)
    ResponseEntity<ApiError> handleStudentInviteAlreadyAccepted(StudentInviteAlreadyAcceptedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiError.of(
                        "STUDENT_INVITE_ALREADY_ACCEPTED",
                        "Student invitation has already been accepted",
                        MDC.get("traceId")
                )
        );
    }

    @ExceptionHandler(PublicStudentInviteAlreadyAcceptedException.class)
    ResponseEntity<ApiError> handlePublicStudentInviteAlreadyAccepted(
            PublicStudentInviteAlreadyAcceptedException exception
    ) {
        return gone("STUDENT_INVITE_ALREADY_ACCEPTED", "Student invitation has already been accepted");
    }

    @ExceptionHandler(StudentInviteExpiredException.class)
    ResponseEntity<ApiError> handleStudentInviteExpired(StudentInviteExpiredException exception) {
        return gone("STUDENT_INVITE_EXPIRED", "Student invitation has expired");
    }

    @ExceptionHandler(StudentInviteRevokedException.class)
    ResponseEntity<ApiError> handleStudentInviteRevoked(StudentInviteRevokedException exception) {
        return gone("STUDENT_INVITE_REVOKED", "Student invitation has been revoked");
    }

    @ExceptionHandler(StudentInviteEmailConflictException.class)
    ResponseEntity<ApiError> handleStudentInviteEmailConflict(StudentInviteEmailConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiError.of(
                        "STUDENT_INVITE_EMAIL_CONFLICT",
                        "Student invitation email is already registered",
                        MDC.get("traceId")
                )
        );
    }

    private ResponseEntity<ApiError> gone(String code, String message) {
        return ResponseEntity.status(HttpStatus.GONE).body(ApiError.of(code, message, MDC.get("traceId")));
    }
}
