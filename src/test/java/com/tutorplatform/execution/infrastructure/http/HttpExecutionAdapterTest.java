package com.tutorplatform.execution.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.tutorplatform.execution.application.ExecutionComparisonMode;
import com.tutorplatform.execution.application.ExecutionLanguage;
import com.tutorplatform.execution.application.ExecutionRequest;
import com.tutorplatform.execution.application.ExecutionStatus;
import com.tutorplatform.execution.application.ExecutionTestCase;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.client.RestClient;

class HttpExecutionAdapterTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private HttpServer server;
    private URI baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        baseUrl = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void sendsInternalWorkerContractWithLanguageModesLimitsAndBoundedOutput() throws Exception {
        var captured = new AtomicReference<JsonNode>();
        var capturedTraceId = new AtomicReference<String>();
        var request =
                request(
                        List.of(
                                testCase(ExecutionComparisonMode.EXACT, "hidden-exact"),
                                testCase(ExecutionComparisonMode.NORMALIZED, "hidden-normalized")));
        server.createContext(
                "/internal/v1/executions",
                exchange -> {
                    captured.set(OBJECT_MAPPER.readTree(exchange.getRequestBody()));
                    capturedTraceId.set(exchange.getRequestHeaders().getFirst("X-Trace-Id"));
                    respond(exchange, 200, response(request, "PASSED", "abcdef", "uvwxyz"));
                });
        server.start();

        MDC.put("traceId", "trace-from-backend");
        com.tutorplatform.execution.application.ExecutionResult result;
        try {
            result =
                    adapter(baseUrl, Duration.ofMillis(200), Duration.ofSeconds(1), 4)
                            .execute(request);
        } finally {
            MDC.remove("traceId");
        }

        var json = captured.get();
        assertThat(json.path("executionId").asText()).isEqualTo(request.executionId().toString());
        assertThat(json.path("language").asText()).isEqualTo("PYTHON");
        assertThat(json.path("sourceCode").asText()).isEqualTo(request.sourceCode());
        assertThat(json.path("timeLimitMs").asInt()).isEqualTo(5_000);
        assertThat(json.path("memoryLimitMb").asInt()).isEqualTo(128);
        assertThat(json.path("outputLimitBytes").asInt()).isEqualTo(4);
        assertThat(json.path("testCases").get(0).path("comparisonMode").asText())
                .isEqualTo("EXACT");
        assertThat(json.path("testCases").get(1).path("comparisonMode").asText())
                .isEqualTo("NORMALIZED");
        assertThat(json.has("datasourceUrl")).isFalse();
        assertThat(json.has("databaseUsername")).isFalse();
        assertThat(json.has("databasePassword")).isFalse();
        assertThat(json.has("authSession")).isFalse();
        assertThat(capturedTraceId.get()).isEqualTo("trace-from-backend");
        assertThat(result.stdoutExcerpt()).isEqualTo("abcd");
        assertThat(result.stderrExcerpt()).isEqualTo("uvwx");
    }

    @ParameterizedTest
    @EnumSource(ExecutionStatus.class)
    void mapsEveryWorkerStatus(ExecutionStatus status) throws Exception {
        var request = request(List.of(testCase(ExecutionComparisonMode.EXACT, "expected")));
        server.createContext(
                "/internal/v1/executions",
                exchange -> respond(exchange, 200, response(request, status.name(), "out", null)));
        server.start();

        var result = adapter(baseUrl).execute(request);

        assertThat(result.status()).isEqualTo(status);
        assertThat(result.passedTests()).isEqualTo(1);
        assertThat(result.totalTests()).isEqualTo(1);
        assertThat(result.testResults())
                .singleElement()
                .satisfies(
                        testResult -> {
                            assertThat(testResult.testCaseId())
                                    .isEqualTo(request.testCases().getFirst().id());
                            assertThat(testResult.passed()).isTrue();
                        });
    }

    @Test
    void unavailableWorkerMapsToSystemError() throws Exception {
        int unusedPort;
        try (var socket = new ServerSocket(0)) {
            unusedPort = socket.getLocalPort();
        }
        var request = request(List.of(testCase(ExecutionComparisonMode.EXACT, "expected")));

        var result = adapter(URI.create("http://127.0.0.1:" + unusedPort)).execute(request);

        assertThat(result.status()).isEqualTo(ExecutionStatus.SYSTEM_ERROR);
    }

    @Test
    void connectionTimeoutMapsToSystemError() {
        var request = request(List.of(testCase(ExecutionComparisonMode.EXACT, "expected")));

        var result =
                adapter(
                                URI.create("http://192.0.2.1:81"),
                                Duration.ofMillis(25),
                                Duration.ofMillis(25),
                                64)
                        .execute(request);

        assertThat(result.status()).isEqualTo(ExecutionStatus.SYSTEM_ERROR);
    }

    @Test
    void readTimeoutMapsToSystemError() {
        var request =
                new ExecutionRequest(
                        UUID.randomUUID(),
                        ExecutionLanguage.PYTHON,
                        "print(input())",
                        100,
                        128,
                        List.of(testCase(ExecutionComparisonMode.EXACT, "expected")));
        server.createContext(
                "/internal/v1/executions",
                exchange -> {
                    try {
                        Thread.sleep(350);
                        respond(exchange, 200, response(request, "PASSED", null, null));
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                });
        server.start();

        var result =
                adapter(baseUrl, Duration.ofMillis(100), Duration.ofMillis(25), 64)
                        .execute(request);

        assertThat(result.status()).isEqualTo(ExecutionStatus.SYSTEM_ERROR);
    }

    @Test
    void malformedResponseNeverMapsToSolutionStatus() {
        var request = request(List.of(testCase(ExecutionComparisonMode.EXACT, "expected")));
        server.createContext(
                "/internal/v1/executions",
                exchange -> respond(exchange, 200, "{\"status\":\"PASSED\"}"));
        server.start();

        var result = adapter(baseUrl).execute(request);

        assertThat(result.status()).isEqualTo(ExecutionStatus.SYSTEM_ERROR);
    }

    @Test
    void logsCorrelationAndStatusWithoutSourceOrHiddenExpectedOutput() {
        var request =
                request(List.of(testCase(ExecutionComparisonMode.EXACT, "do-not-log-hidden")));
        server.createContext(
                "/internal/v1/executions",
                exchange -> respond(exchange, 200, response(request, "PASSED", null, null)));
        server.start();
        var logger = (Logger) LoggerFactory.getLogger(HttpExecutionAdapter.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        try {
            adapter(baseUrl).execute(request);
        } finally {
            logger.detachAppender(appender);
        }

        var messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages)
                .anyMatch(message -> message.contains(request.executionId().toString()));
        assertThat(messages).anyMatch(message -> message.contains("status=PASSED"));
        assertThat(messages).noneMatch(message -> message.contains(request.sourceCode()));
        assertThat(messages).noneMatch(message -> message.contains("do-not-log-hidden"));
    }

    @Test
    void truncatesUtf8WithoutSplittingCodePoints() {
        assertThat(BoundedOutput.truncateUtf8("ééé", 4)).isEqualTo("éé");
    }

    private HttpExecutionAdapter adapter(URI uri) {
        return adapter(uri, Duration.ofMillis(200), Duration.ofSeconds(1), 64);
    }

    private HttpExecutionAdapter adapter(
            URI uri, Duration connectTimeout, Duration readTimeout, int maxBytes) {
        var properties =
                new ExecutionProperties(
                        new ExecutionProperties.Worker(uri, connectTimeout, readTimeout),
                        new ExecutionProperties.Output(maxBytes));
        return new HttpExecutionAdapter(RestClient.builder(), properties);
    }

    private static ExecutionRequest request(List<ExecutionTestCase> testCases) {
        return new ExecutionRequest(
                UUID.randomUUID(),
                ExecutionLanguage.PYTHON,
                "print(input())",
                5_000,
                128,
                testCases);
    }

    private static ExecutionTestCase testCase(ExecutionComparisonMode mode, String expectedOutput) {
        return new ExecutionTestCase(UUID.randomUUID(), "hello", expectedOutput, mode);
    }

    private static String response(
            ExecutionRequest request, String status, String stdout, String stderr) {
        return """
            {
              "executionId": "%s",
              "status": "%s",
              "passedTests": %d,
              "totalTests": %d,
              "executionTimeMs": 42,
              "stdoutExcerpt": %s,
              "stderrExcerpt": %s,
              "testResults": [{
                "testCaseId": "%s",
                "passed": true,
                "executionTimeMs": 42,
                "stdoutExcerpt": %s,
                "stderrExcerpt": %s
              }]
            }
            """
                .formatted(
                        request.executionId(),
                        status,
                        request.testCases().size(),
                        request.testCases().size(),
                        jsonString(stdout),
                        jsonString(stderr),
                        request.testCases().getFirst().id(),
                        jsonString(stdout),
                        jsonString(stderr));
    }

    private static String jsonString(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
