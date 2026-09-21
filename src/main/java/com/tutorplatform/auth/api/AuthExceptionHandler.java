package com.tutorplatform.auth.api;

import com.tutorplatform.auth.application.EmailAlreadyRegisteredException;
import com.tutorplatform.auth.application.InvalidCredentialsException;
import com.tutorplatform.platform.application.RegistrationInviteRequiredException;
import com.tutorplatform.shared.api.ApiError;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(0)
public class AuthExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ResponseEntity<ApiError> handleEmailAlreadyRegistered(
            EmailAlreadyRegisteredException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                        ApiError.of(
                                "EMAIL_ALREADY_REGISTERED",
                                "Email is already registered",
                                MDC.get("traceId")));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(
                        ApiError.of(
                                "AUTH_INVALID_CREDENTIALS",
                                "Invalid email or password",
                                MDC.get("traceId")));
    }

    @ExceptionHandler(RegistrationInviteRequiredException.class)
    ResponseEntity<ApiError> handleRegistrationInviteRequired(
            RegistrationInviteRequiredException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                        ApiError.of(
                                "REGISTRATION_INVITE_REQUIRED",
                                "Registration is available by invitation only",
                                MDC.get("traceId")));
    }
}
