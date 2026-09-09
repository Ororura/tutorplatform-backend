package com.tutorplatform.execution.application;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionPortTest {

    @Test
    void portAcceptsTransportNeutralExecutionRequest() {
        var request = new ExecutionRequest(
            UUID.randomUUID(),
            ExecutionLanguage.PYTHON,
            "print(input())",
            5_000,
            128,
            List.of(new ExecutionTestCase(
                UUID.randomUUID(), "hello", "hello", ExecutionComparisonMode.NORMALIZED
            ))
        );
        ExecutionPort port = received -> ExecutionResult.systemError(received.executionId());

        var result = port.execute(request);

        assertThat(result.executionId()).isEqualTo(request.executionId());
        assertThat(result.status()).isEqualTo(ExecutionStatus.SYSTEM_ERROR);
    }
}
