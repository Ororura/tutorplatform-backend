package com.tutorplatform.auth.infrastructure.ratelimit;

import com.tutorplatform.shared.api.ApiError;
import com.tutorplatform.shared.api.ApiErrorWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

public final class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final List<ProtectedEndpoint> ENDPOINTS =
            List.of(
                    endpoint("/api/v1/auth/login", AuthRateLimiter.Operation.LOGIN),
                    endpoint(
                            "/api/v1/auth/register/teacher",
                            AuthRateLimiter.Operation.TEACHER_REGISTRATION),
                    endpoint(
                            "/api/v1/public/student-invitations/*/accept",
                            AuthRateLimiter.Operation.STUDENT_INVITATION),
                    endpoint(
                            "/api/v1/public/teacher-invitations/*/accept",
                            AuthRateLimiter.Operation.TEACHER_INVITATION));

    private final AuthRateLimiter limiter;
    private final ApiErrorWriter errorWriter;

    public AuthRateLimitFilter(AuthRateLimiter limiter, ApiErrorWriter errorWriter) {
        this.limiter = limiter;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain)
            throws ServletException, IOException {
        for (ProtectedEndpoint endpoint : ENDPOINTS) {
            if (endpoint.matcher().matches(request)) {
                // The servlet container resolves the peer. Never parse arbitrary forwarded headers.
                long retryAfter = limiter.acquire(endpoint.operation(), request.getRemoteAddr());
                if (retryAfter > 0) {
                    response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter));
                    response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
                    errorWriter.write(
                            response,
                            429,
                            ApiError.of(
                                    "RATE_LIMIT_EXCEEDED",
                                    "Too many authentication requests. Try again later.",
                                    MDC.get("traceId")));
                    return;
                }
                break;
            }
        }
        chain.doFilter(request, response);
    }

    private static ProtectedEndpoint endpoint(String path, AuthRateLimiter.Operation operation) {
        return new ProtectedEndpoint(
                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, path), operation);
    }

    private record ProtectedEndpoint(RequestMatcher matcher, AuthRateLimiter.Operation operation) {}
}
