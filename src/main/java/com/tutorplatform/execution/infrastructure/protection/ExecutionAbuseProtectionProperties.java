package com.tutorplatform.execution.infrastructure.protection;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("execution.abuse-protection")
public record ExecutionAbuseProtectionProperties(
        @DefaultValue("10") int limit,
        @DefaultValue("1m") Duration window,
        @DefaultValue("10000") int maxBuckets,
        @DefaultValue("30s") Duration cleanupInterval) {
    public ExecutionAbuseProtectionProperties {
        if (limit < 1 || maxBuckets < 1) {
            throw new IllegalArgumentException("Execution limit and max-buckets must be positive");
        }
        requirePositive(window);
        requirePositive(cleanupInterval);
    }

    private static void requirePositive(Duration value) {
        if (value == null || value.toMillis() < 1) {
            throw new IllegalArgumentException(
                    "Execution protection durations must be at least 1ms");
        }
        value.toNanos();
    }
}
