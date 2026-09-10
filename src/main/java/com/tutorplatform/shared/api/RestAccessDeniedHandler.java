package com.tutorplatform.shared.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ApiErrorWriter writer;

    public RestAccessDeniedHandler(ApiErrorWriter writer) {
        this.writer = writer;
    }

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException {
        boolean csrfFailure = accessDeniedException instanceof MissingCsrfTokenException
            || accessDeniedException instanceof InvalidCsrfTokenException;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean unauthenticated = authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken;
        if (csrfFailure && unauthenticated
            && request.getRequestURI().startsWith("/api/v1/teacher/reports")) {
            writer.write(
                response,
                HttpStatus.UNAUTHORIZED.value(),
                ApiError.of("AUTH_REQUIRED", "Authentication is required", MDC.get("traceId"))
            );
            return;
        }

        String code = csrfFailure ? "CSRF_INVALID" : "ACCESS_DENIED";
        String message = csrfFailure ? "Missing or invalid CSRF token" : "Access denied";

        writer.write(
            response,
            HttpStatus.FORBIDDEN.value(),
            ApiError.of(code, message, MDC.get("traceId"))
        );
    }
}
