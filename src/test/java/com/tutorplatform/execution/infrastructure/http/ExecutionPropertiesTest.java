package com.tutorplatform.execution.infrastructure.http;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecutionPropertiesTest {

    @Test
    void baseUrlAndTimeoutsAreConfigurableAndExecutionWaitIsBounded() {
        var worker = new ExecutionProperties.Worker(
            URI.create("http://worker.internal:8090"),
            Duration.ofSeconds(2),
            Duration.ofSeconds(5)
        );

        assertThat(worker.baseUrl()).hasToString("http://worker.internal:8090");
        assertThat(worker.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(worker.timeoutFor(30_000)).isEqualTo(Duration.ofSeconds(35));
        assertThat(new ExecutionProperties.Worker(
            URI.create("http://worker.internal:8090"), Duration.ofSeconds(2), Duration.ofSeconds(40)
        ).timeoutFor(30_000)).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void rejectsUnboundedTimeouts() {
        assertThatThrownBy(() -> new ExecutionProperties.Worker(
            URI.create("http://worker.internal:8090"), Duration.ofSeconds(2), Duration.ofSeconds(61)
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
