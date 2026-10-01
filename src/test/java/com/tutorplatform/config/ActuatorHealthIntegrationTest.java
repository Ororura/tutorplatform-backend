package com.tutorplatform.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tutorplatform.test.PostgresIntegrationTest;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.boot.jdbc.health.DataSourceHealthIndicator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "management.prometheus.metrics.export.enabled=true")
@AutoConfigureMockMvc
class ActuatorHealthIntegrationTest extends PostgresIntegrationTest {

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_actuator_health", null);
        // No execution worker is running at this address. Core backend readiness must still be UP.
        registry.add("execution.worker.base-url", () -> "http://127.0.0.1:1");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ApplicationContext context;
    @Autowired private ApplicationAvailability availability;
    @Autowired private HealthContributorRegistry contributors;
    @Autowired private WebEndpointsSupplier webEndpoints;

    @Test
    void onlyHealthAndPrometheusEndpointsAreExposed() {
        assertThat(webEndpoints.getEndpoints())
                .extracting(endpoint -> endpoint.getEndpointId().toString())
                .containsExactlyInAnyOrder("health", "prometheus");
        assertThat(context.containsBean("environmentEndpoint")).isFalse();
        assertThat(context.containsBean("beansEndpoint")).isFalse();
        assertThat(context.containsBean("loggersEndpoint")).isFalse();
    }

    @Test
    void livenessIsUpAfterNormalStartup() throws Exception {
        assertThat(availability.getLivenessState()).isEqualTo(LivenessState.CORRECT);
        assertUp("/actuator/health/liveness");
    }

    @Test
    void readinessIsUpWithRealPostgresAndNoExecutionWorker() throws Exception {
        assertThat(availability.getReadinessState()).isEqualTo(ReadinessState.ACCEPTING_TRAFFIC);
        assertThat(contributors.getContributor("db")).isInstanceOf(DataSourceHealthIndicator.class);
        assertUp("/actuator/health/readiness");
        assertUp("/actuator/health");
    }

    @Test
    void databaseFailureMakesReadinessDownWhileLivenessStaysUp() throws Exception {
        var unavailableDataSource = mock(DataSource.class);
        when(unavailableDataSource.getConnection())
                .thenThrow(new SQLException("Simulated PostgreSQL outage"));
        var original = contributors.unregisterContributor("db");
        assertThat(original).isNotNull();
        contributors.registerContributor(
                "db", new DataSourceHealthIndicator(unavailableDataSource));
        try {
            mockMvc.perform(get("/actuator/health/readiness"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().json("{\"status\":\"DOWN\"}"))
                    .andExpect(jsonPath("$.components").doesNotExist())
                    .andExpect(jsonPath("$.details").doesNotExist());
            assertUp("/actuator/health/liveness");
        } finally {
            contributors.unregisterContributor("db");
            contributors.registerContributor("db", original);
        }
        assertUp("/actuator/health/readiness");
    }

    @Test
    void unrelatedDownContributorDoesNotTakeEitherProbeDown() throws Exception {
        contributors.registerContributor(
                "executionWorker", (HealthIndicator) () -> Health.down().build());
        try {
            mockMvc.perform(get("/actuator/health")).andExpect(status().isServiceUnavailable());
            assertUp("/actuator/health/liveness");
            assertUp("/actuator/health/readiness");
        } finally {
            contributors.unregisterContributor("executionWorker");
        }
    }

    @Test
    void refusingTrafficMakesReadinessUnavailableWhileLivenessStaysUp() throws Exception {
        AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);
        try {
            mockMvc.perform(get("/actuator/health/readiness"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value("OUT_OF_SERVICE"));
            assertUp("/actuator/health/liveness");
        } finally {
            AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
        }
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/actuator/health",
                "/actuator/health/liveness",
                "/actuator/health/readiness"
            })
    void authenticatedUsersDoNotReceiveHealthDetails(String path) throws Exception {
        mockMvc.perform(get(path).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/actuator",
                "/actuator/env",
                "/actuator/configprops",
                "/actuator/beans",
                "/actuator/metrics",
                "/actuator/info",
                "/actuator/loggers",
                "/actuator/heapdump",
                "/actuator/threaddump",
                "/actuator/mappings",
                "/actuator/shutdown",
                "/actuator/health/db",
                "/actuator/health/readiness/db",
                "/actuator/health/liveness/livenessState"
            })
    void actuatorAdministrativeAndComponentPathsAreDeniedEvenToApplicationAdmins(String path)
            throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(path).with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/actuator/health",
                "/actuator/health/liveness",
                "/actuator/health/readiness",
                "/actuator/prometheus",
                "/actuator/shutdown"
            })
    void actuatorUnsafeMethodsAreDeniedEvenWithCsrf(String path) throws Exception {
        mockMvc.perform(post(path).with(csrf())).andExpect(status().isUnauthorized());
        mockMvc.perform(post(path).with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void prometheusScrapeRemainsPublicAndExportsJvmMetrics() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "jvm_memory_used_bytes")));
    }

    private void assertUp(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }
}
