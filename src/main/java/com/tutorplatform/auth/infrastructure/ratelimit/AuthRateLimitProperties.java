package com.tutorplatform.auth.infrastructure.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.security.auth-rate-limit")
public record AuthRateLimitProperties(
        @DefaultValue("10") int loginLimit,
        @DefaultValue("1m") Duration loginWindow,
        @DefaultValue("5") int registrationLimit,
        @DefaultValue("10m") Duration registrationWindow,
        @DefaultValue("10") int studentInvitationLimit,
        @DefaultValue("10") int teacherInvitationLimit,
        @DefaultValue("10m") Duration invitationWindow,
        @DefaultValue("10000") int maxBuckets,
        @DefaultValue("30s") Duration cleanupInterval) {

    public AuthRateLimitProperties {
        if (loginLimit < 1
                || registrationLimit < 1
                || studentInvitationLimit < 1
                || teacherInvitationLimit < 1
                || maxBuckets < 1) {
            throw new IllegalArgumentException("Auth rate limits and max-buckets must be positive");
        }
        requirePositive(loginWindow);
        requirePositive(registrationWindow);
        requirePositive(invitationWindow);
        requirePositive(cleanupInterval);
    }

    private static void requirePositive(Duration duration) {
        if (duration == null || duration.toMillis() < 1) {
            throw new IllegalArgumentException("Auth rate limit durations must be at least 1ms");
        }
        // Reject durations that cannot be represented by the monotonic ticker.
        duration.toNanos();
    }

    int limit(AuthRateLimiter.Operation operation) {
        return switch (operation) {
            case LOGIN -> loginLimit;
            case TEACHER_REGISTRATION -> registrationLimit;
            case STUDENT_INVITATION -> studentInvitationLimit;
            case TEACHER_INVITATION -> teacherInvitationLimit;
        };
    }

    Duration window(AuthRateLimiter.Operation operation) {
        return switch (operation) {
            case LOGIN -> loginWindow;
            case TEACHER_REGISTRATION -> registrationWindow;
            case STUDENT_INVITATION, TEACHER_INVITATION -> invitationWindow;
        };
    }
}
