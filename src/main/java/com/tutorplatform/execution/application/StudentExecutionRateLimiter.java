package com.tutorplatform.execution.application;

import com.tutorplatform.execution.infrastructure.protection.ExecutionAbuseProtectionProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Single-instance fixed windows shared by Run and code Submit, keyed by the owned student ID. */
@Component
public final class StudentExecutionRateLimiter {
    private final ExecutionAbuseProtectionProperties properties;
    private final LongSupplier ticker;
    private final Map<UUID, Bucket> buckets = new HashMap<>();

    @Autowired
    public StudentExecutionRateLimiter(ExecutionAbuseProtectionProperties properties) {
        this(properties, System::nanoTime);
    }

    StudentExecutionRateLimiter(
            ExecutionAbuseProtectionProperties properties, LongSupplier ticker) {
        this.properties = properties;
        this.ticker = ticker;
    }

    /** Reject before creating a submission or entering the worker infrastructure error handler. */
    public synchronized void acquire(UUID studentId) {
        Objects.requireNonNull(studentId, "studentId");
        long now = ticker.getAsLong();
        Bucket bucket = buckets.get(studentId);
        if (bucket != null && expired(bucket, now)) {
            buckets.remove(studentId);
            bucket = null;
        }
        if (bucket == null) {
            if (buckets.size() >= properties.maxBuckets()) {
                removeExpired(now);
            }
            if (buckets.size() >= properties.maxBuckets()) {
                // Never evict an active student: that would reset their quota under pressure.
                long remaining =
                        buckets.values().stream()
                                .mapToLong(
                                        value ->
                                                properties.window().toNanos()
                                                        - (now - value.startedAt))
                                .min()
                                .orElseThrow();
                throw exceeded(remaining);
            }
            bucket = new Bucket(now);
            buckets.put(studentId, bucket);
        }
        if (bucket.requests >= properties.limit()) {
            throw exceeded(properties.window().toNanos() - (now - bucket.startedAt));
        }
        bucket.requests++;
    }

    @Scheduled(fixedDelayString = "${execution.abuse-protection.cleanup-interval:30s}")
    public synchronized void cleanup() {
        removeExpired(ticker.getAsLong());
    }

    private void removeExpired(long now) {
        buckets.values().removeIf(bucket -> expired(bucket, now));
    }

    private boolean expired(Bucket bucket, long now) {
        return now - bucket.startedAt >= properties.window().toNanos();
    }

    private static ExecutionRateLimitExceededException exceeded(long remainingNanos) {
        return new ExecutionRateLimitExceededException(1 + (remainingNanos - 1) / 1_000_000_000);
    }

    private static final class Bucket {
        private final long startedAt;
        private int requests;

        private Bucket(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}
