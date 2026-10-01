package com.tutorplatform.execution.application;

import static org.assertj.core.api.Assertions.*;

import com.tutorplatform.execution.infrastructure.protection.ExecutionAbuseProtectionProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class StudentExecutionRateLimiterTest {
    private final AtomicLong ticker = new AtomicLong();
    private final StudentExecutionRateLimiter limiter =
            new StudentExecutionRateLimiter(
                    new ExecutionAbuseProtectionProperties(
                            2, Duration.ofSeconds(60), 2, Duration.ofSeconds(30), 65536),
                    ticker::get);

    @Test
    void permitsQuotaAndRejectsBurstUntilExactWindowBoundary() {
        UUID student = UUID.randomUUID();
        limiter.acquire(student);
        limiter.acquire(student);
        assertRetryAfter(student, 60);
        ticker.set(Duration.ofSeconds(59).toNanos() + 1);
        assertRetryAfter(student, 1);
        ticker.set(Duration.ofSeconds(60).toNanos());
        limiter.acquire(student);
        limiter.acquire(student);
        assertRetryAfter(student, 60);
    }

    @Test
    void differentStudentsHaveIndependentQuotas() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        limiter.acquire(first);
        limiter.acquire(first);
        assertRetryAfter(first, 60);
        limiter.acquire(second);
        limiter.acquire(second);
        assertRetryAfter(second, 60);
    }

    @Test
    void capacityNeverEvictsActiveBucketsAndExpiredCapacityIsReused() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        limiter.acquire(first);
        limiter.acquire(first);
        ticker.set(Duration.ofSeconds(10).toNanos());
        limiter.acquire(second);
        for (int i = 0; i < 100; i++) {
            assertRetryAfter(UUID.randomUUID(), 50);
        }
        assertRetryAfter(first, 50);
        // Capacity remains usable by an already tracked student.
        limiter.acquire(second);
        ticker.set(Duration.ofSeconds(60).toNanos());
        limiter.acquire(UUID.randomUUID());
        assertRetryAfter(second, 10);
        assertRetryAfter(UUID.randomUUID(), 10);
    }

    @Test
    void cleanupReclaimsOnlyExpiredBuckets() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        limiter.acquire(first);
        ticker.set(Duration.ofSeconds(10).toNanos());
        limiter.acquire(second);
        limiter.acquire(second);
        ticker.set(Duration.ofSeconds(60).toNanos());
        limiter.cleanup();
        limiter.acquire(UUID.randomUUID());
        assertRetryAfter(second, 10);
        assertRetryAfter(UUID.randomUUID(), 10);
    }

    @Test
    void concurrentRequestsCannotExceedStudentQuota() throws Exception {
        UUID student = UUID.randomUUID();
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var attempts = new ArrayList<Future<Boolean>>();
            for (int i = 0; i < 40; i++) {
                attempts.add(
                        executor.submit(
                                () -> {
                                    start.await();
                                    try {
                                        limiter.acquire(student);
                                        return true;
                                    } catch (ExecutionRateLimitExceededException exception) {
                                        return false;
                                    }
                                }));
            }
            start.countDown();
            int accepted = 0;
            for (var attempt : attempts) {
                if (attempt.get()) {
                    accepted++;
                }
            }
            assertThat(accepted).isEqualTo(2);
        }
    }

    private void assertRetryAfter(UUID student, long seconds) {
        assertThatThrownBy(() -> limiter.acquire(student))
                .isInstanceOfSatisfying(
                        ExecutionRateLimitExceededException.class,
                        exception -> assertThat(exception.retryAfterSeconds()).isEqualTo(seconds));
    }
}
