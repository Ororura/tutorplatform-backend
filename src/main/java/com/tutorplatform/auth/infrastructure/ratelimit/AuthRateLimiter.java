package com.tutorplatform.auth.infrastructure.ratelimit;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import org.springframework.scheduling.annotation.Scheduled;

/** Single-instance fixed windows. Keys contain only an operation and a servlet client address. */
public final class AuthRateLimiter {
    public enum Operation {
        LOGIN,
        TEACHER_REGISTRATION,
        STUDENT_INVITATION,
        TEACHER_INVITATION
    }

    private final AuthRateLimitProperties properties;
    private final LongSupplier ticker;
    private final Map<Key, Bucket> buckets = new HashMap<>();

    public AuthRateLimiter(AuthRateLimitProperties properties) {
        this(properties, System::nanoTime);
    }

    AuthRateLimiter(AuthRateLimitProperties properties, LongSupplier ticker) {
        this.properties = properties;
        this.ticker = ticker;
    }

    /** Returns zero when allowed, otherwise the Retry-After value in seconds. */
    public synchronized long acquire(Operation operation, String clientAddress) {
        long now = ticker.getAsLong();
        Key key = new Key(operation, clientAddress);
        Bucket bucket = buckets.get(key);
        if (bucket != null && expired(bucket, now)) {
            buckets.remove(key);
            bucket = null;
        }
        if (bucket == null) {
            if (buckets.size() >= properties.maxBuckets()) {
                removeExpired(now);
            }
            if (buckets.size() >= properties.maxBuckets()) {
                // Do not evict active buckets: rotating addresses must not reset existing limits.
                long remaining =
                        buckets.values().stream()
                                .mapToLong(value -> value.windowNanos - (now - value.startedAt))
                                .min()
                                .orElseThrow();
                return retryAfter(remaining);
            }
            bucket = new Bucket(now, properties.window(operation).toNanos());
            buckets.put(key, bucket);
        }
        if (bucket.requests >= properties.limit(operation)) {
            return retryAfter(bucket.windowNanos - (now - bucket.startedAt));
        }
        bucket.requests++;
        return 0;
    }

    @Scheduled(fixedDelayString = "${app.security.auth-rate-limit.cleanup-interval:30s}")
    public synchronized void cleanup() {
        removeExpired(ticker.getAsLong());
    }

    private void removeExpired(long now) {
        buckets.values().removeIf(bucket -> expired(bucket, now));
    }

    private static boolean expired(Bucket bucket, long now) {
        return now - bucket.startedAt >= bucket.windowNanos;
    }

    private static long retryAfter(long remainingNanos) {
        return 1 + (remainingNanos - 1) / 1_000_000_000;
    }

    private record Key(Operation operation, String clientAddress) {}

    private static final class Bucket {
        private final long startedAt;
        private final long windowNanos;
        private int requests;

        private Bucket(long startedAt, long windowNanos) {
            this.startedAt = startedAt;
            this.windowNanos = windowNanos;
        }
    }
}
