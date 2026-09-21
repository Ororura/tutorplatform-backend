package com.tutorplatform.execution.infrastructure.http;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "execution")
public record ExecutionProperties(@NotNull @Valid Worker worker, @NotNull @Valid Output output) {
    private static final Duration MAX_HTTP_TIMEOUT = Duration.ofSeconds(60);

    public record Worker(
            @NotNull URI baseUrl, @NotNull Duration connectTimeout, @NotNull Duration readTimeout) {
        public Worker {
            if (baseUrl != null && (!baseUrl.isAbsolute() || baseUrl.getHost() == null)) {
                throw new IllegalArgumentException(
                        "execution.worker.base-url must be an absolute HTTP URL");
            }
            if (baseUrl != null
                    && !"http".equals(baseUrl.getScheme())
                    && !"https".equals(baseUrl.getScheme())) {
                throw new IllegalArgumentException(
                        "execution.worker.base-url must use HTTP or HTTPS");
            }
            requirePositiveBounded(connectTimeout, "execution.worker.connect-timeout");
            requirePositiveBounded(readTimeout, "execution.worker.read-timeout");
        }

        Duration timeoutFor(int timeLimitMs) {
            var timeout = Duration.ofMillis(timeLimitMs).plus(readTimeout);
            return timeout.compareTo(MAX_HTTP_TIMEOUT) > 0 ? MAX_HTTP_TIMEOUT : timeout;
        }

        private static void requirePositiveBounded(Duration value, String name) {
            if (value != null
                    && (value.isZero()
                            || value.isNegative()
                            || value.compareTo(MAX_HTTP_TIMEOUT) > 0)) {
                throw new IllegalArgumentException(
                        name + " must be positive and at most 60 seconds");
            }
        }
    }

    public record Output(@Min(1) @Max(1_048_576) int maxBytes) {}
}
