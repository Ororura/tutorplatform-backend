package com.tutorplatform.platform.api;

import com.tutorplatform.platform.application.invite.TeacherInvitationEmailAlreadyRegisteredException;
import com.tutorplatform.platform.application.invite.TeacherInvitationNotActiveException;
import com.tutorplatform.platform.application.invite.TeacherInvitationNotFoundException;
import com.tutorplatform.shared.api.ApiError;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(0)
public class TeacherInvitationExceptionHandler {

    @ExceptionHandler(TeacherInvitationNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(TeacherInvitationNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                        ApiError.of(
                                "TEACHER_INVITATION_NOT_FOUND",
                                exception.getMessage(),
                                MDC.get("traceId")));
    }

    @ExceptionHandler(TeacherInvitationNotActiveException.class)
    ResponseEntity<ApiError> handleNotActive(TeacherInvitationNotActiveException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                        ApiError.of(
                                "TEACHER_INVITATION_NOT_ACTIVE",
                                exception.getMessage(),
                                MDC.get("traceId")));
    }

    @ExceptionHandler(TeacherInvitationEmailAlreadyRegisteredException.class)
    ResponseEntity<ApiError> handleEmailRegistered(
            TeacherInvitationEmailAlreadyRegisteredException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                        ApiError.of(
                                "EMAIL_ALREADY_REGISTERED",
                                exception.getMessage(),
                                MDC.get("traceId")));
    }
}
