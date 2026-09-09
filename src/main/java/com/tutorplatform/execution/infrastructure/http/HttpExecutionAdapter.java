package com.tutorplatform.execution.infrastructure.http;

import com.tutorplatform.execution.application.ExecutionPort;
import com.tutorplatform.execution.application.ExecutionRequest;
import com.tutorplatform.execution.application.ExecutionResult;
import com.tutorplatform.execution.application.ExecutionStatus;
import com.tutorplatform.execution.application.ExecutionTestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

@Component
public class HttpExecutionAdapter implements ExecutionPort {
    private static final Logger log = LoggerFactory.getLogger(HttpExecutionAdapter.class);
    private static final String EXECUTIONS_PATH = "/internal/v1/executions";

    private final RestClient.Builder restClientBuilder;
    private final ExecutionProperties properties;

    public HttpExecutionAdapter(RestClient.Builder restClientBuilder, ExecutionProperties properties) {
        this.restClientBuilder = restClientBuilder;
        this.properties = properties;
    }

    private RestClient restClient(ExecutionRequest request) {
        var httpClient = HttpClient.newBuilder()
            .connectTimeout(properties.worker().connectTimeout())
            .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.worker().timeoutFor(request.timeLimitMs()));
        return restClientBuilder.clone()
            .baseUrl(properties.worker().baseUrl().toString())
            .requestFactory(requestFactory)
            .build();
    }

    @Override
    public ExecutionResult execute(ExecutionRequest request) {
        var startedAt = System.nanoTime();
        log.info("Worker execution request started: executionId={}", request.executionId());
        try {
            var response = restClient(request).post()
                .uri(EXECUTIONS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(WorkerExecutionRequest.from(request, properties.output().maxBytes()))
                .retrieve()
                .body(WorkerExecutionResponse.class);
            var result = mapResponse(request, response);
            log.info(
                "Worker execution request completed: executionId={}, durationMs={}, status={}",
                request.executionId(), elapsedMillis(startedAt), result.status()
            );
            return result;
        } catch (RestClientException | IllegalArgumentException exception) {
            log.warn(
                "Worker execution infrastructure failure: executionId={}, durationMs={}, failureType={}",
                request.executionId(), elapsedMillis(startedAt), exception.getClass().getSimpleName()
            );
            return ExecutionResult.systemError(request.executionId(), request.testCases().size());
        }
    }

    private ExecutionResult mapResponse(ExecutionRequest request, WorkerExecutionResponse response) {
        if (response == null || !request.executionId().equals(response.executionId())) {
            throw new IllegalArgumentException("Worker response has an invalid executionId");
        }
        if (response.status() == null || response.passedTests() == null || response.totalTests() == null
            || response.executionTimeMs() == null || response.testResults() == null) {
            throw new IllegalArgumentException("Worker response is incomplete");
        }
        if (response.totalTests() != request.testCases().size()) {
            throw new IllegalArgumentException("Worker response has an invalid total test count");
        }

        var status = ExecutionStatus.valueOf(response.status());
        var testResults = response.testResults().stream().map(this::mapTestResult).toList();
        return new ExecutionResult(
            response.executionId(),
            status,
            response.passedTests(),
            response.totalTests(),
            response.executionTimeMs(),
            bounded(response.stdoutExcerpt()),
            bounded(response.stderrExcerpt()),
            testResults
        );
    }

    private ExecutionTestResult mapTestResult(WorkerExecutionResponse.TestResult response) {
        if (response == null || response.testCaseId() == null || response.passed() == null
            || response.executionTimeMs() == null) {
            throw new IllegalArgumentException("Worker test result is incomplete");
        }
        return new ExecutionTestResult(
            response.testCaseId(),
            response.passed(),
            response.executionTimeMs(),
            bounded(response.stdoutExcerpt()),
            bounded(response.stderrExcerpt())
        );
    }

    private String bounded(String output) {
        return BoundedOutput.truncateUtf8(output, properties.output().maxBytes());
    }

    private static long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }
}
