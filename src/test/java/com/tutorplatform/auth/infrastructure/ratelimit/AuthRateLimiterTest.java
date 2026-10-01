package com.tutorplatform.auth.infrastructure.ratelimit;

import static com.tutorplatform.auth.infrastructure.ratelimit.AuthRateLimiter.Operation.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class AuthRateLimiterTest {
    private final AtomicLong time = new AtomicLong();
    private final AuthRateLimiter limiter = new AuthRateLimiter(properties(2, 3), time::get);

    static AuthRateLimitProperties properties(int limit, int capacity) {
        return new AuthRateLimitProperties(
                limit,
                Duration.ofSeconds(60),
                limit,
                Duration.ofSeconds(60),
                limit,
                limit,
                Duration.ofSeconds(60),
                capacity,
                Duration.ofSeconds(30));
    }

    @Test
    void allowsLimitThenBlocksUntilTheExactWindowBoundaryWithoutExtendingIt() {
        assertThat(limiter.acquire(LOGIN, "192.0.2.1")).isZero();
        assertThat(limiter.acquire(LOGIN, "192.0.2.1")).isZero();
        assertThat(limiter.acquire(LOGIN, "192.0.2.1")).isEqualTo(60);
        time.set(Duration.ofSeconds(59).toNanos() + 1);
        assertThat(limiter.acquire(LOGIN, "192.0.2.1")).isEqualTo(1);
        time.set(Duration.ofSeconds(60).toNanos());
        assertThat(limiter.acquire(LOGIN, "192.0.2.1")).isZero();
    }

    @Test
    void isolatesAddressesAndOperations() {
        limiter.acquire(LOGIN, "192.0.2.1");
        limiter.acquire(LOGIN, "192.0.2.1");
        assertThat(limiter.acquire(LOGIN, "192.0.2.1")).isPositive();
        assertThat(limiter.acquire(LOGIN, "192.0.2.2")).isZero();
        assertThat(limiter.acquire(TEACHER_REGISTRATION, "192.0.2.1")).isZero();
    }

    @Test
    void capacityFailsClosedWithoutEvictingActiveBucketsAndRecoversAfterExpiry() {
        limiter.acquire(LOGIN, "192.0.2.1");
        limiter.acquire(LOGIN, "192.0.2.1");
        time.set(Duration.ofSeconds(10).toNanos());
        limiter.acquire(LOGIN, "192.0.2.2");
        limiter.acquire(LOGIN, "192.0.2.3");
        assertThat(limiter.acquire(LOGIN, "192.0.2.4")).isEqualTo(50);
        assertThat(limiter.acquire(LOGIN, "192.0.2.1")).isEqualTo(50);
        assertThat(limiter.acquire(LOGIN, "192.0.2.2")).isZero();
        assertThat(limiter).extracting("buckets").asInstanceOf(MAP).hasSize(3);
        time.set(Duration.ofSeconds(60).toNanos());
        assertThat(limiter.acquire(LOGIN, "192.0.2.4")).isZero();
        assertThat(limiter).extracting("buckets").asInstanceOf(MAP).hasSize(3);
    }

    @Test
    void scheduledCleanupRemovesExpiredBucketsEvenWithoutNewRequests() {
        limiter.acquire(LOGIN, "192.0.2.1");
        time.set(Duration.ofSeconds(60).toNanos());
        limiter.cleanup();
        assertThat(limiter).extracting("buckets").asInstanceOf(MAP).isEmpty();
    }

    @Test
    void concurrentRequestsCannotExceedTheLimit() throws Exception {
        var concurrentLimiter = new AuthRateLimiter(properties(10, 100), time::get);
        var start = new CountDownLatch(1);
        List<Future<Long>> results = new ArrayList<>();
        try (var executor = Executors.newFixedThreadPool(20)) {
            for (int i = 0; i < 100; i++) {
                results.add(
                        executor.submit(
                                () -> {
                                    start.await();
                                    return concurrentLimiter.acquire(LOGIN, "192.0.2.1");
                                }));
            }
            start.countDown();
            int allowed = 0;
            for (var result : results) {
                if (result.get(10, TimeUnit.SECONDS) == 0) allowed++;
            }
            assertThat(allowed).isEqualTo(10);
        }
    }
}
