package com.tutorplatform.execution.infrastructure.protection;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class ExecutionAbuseProtectionPropertiesTest {
    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void bindsDefaults() {
        runner.run(
                context -> {
                    assertThat(context).hasNotFailed();
                    var properties = context.getBean(ExecutionAbuseProtectionProperties.class);
                    assertThat(properties.limit()).isEqualTo(10);
                    assertThat(properties.window()).isEqualTo(Duration.ofMinutes(1));
                    assertThat(properties.maxBuckets()).isEqualTo(10000);
                    assertThat(properties.cleanupInterval()).isEqualTo(Duration.ofSeconds(30));
                    assertThat(properties.maxSourceCodeBytes()).isEqualTo(65536);
                });
    }

    @Test
    void bindsOverrides() {
        runner.withPropertyValues(
                        "execution.abuse-protection.limit=3",
                        "execution.abuse-protection.window=2m",
                        "execution.abuse-protection.max-buckets=50",
                        "execution.abuse-protection.cleanup-interval=5s",
                        "execution.abuse-protection.max-source-code-bytes=128")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            var properties =
                                    context.getBean(ExecutionAbuseProtectionProperties.class);
                            assertThat(properties.limit()).isEqualTo(3);
                            assertThat(properties.window()).isEqualTo(Duration.ofMinutes(2));
                            assertThat(properties.maxBuckets()).isEqualTo(50);
                            assertThat(properties.cleanupInterval())
                                    .isEqualTo(Duration.ofSeconds(5));
                            assertThat(properties.maxSourceCodeBytes()).isEqualTo(128);
                        });
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "limit=0",
                "limit=-1",
                "max-buckets=0",
                "window=0s",
                "window=-1s",
                "window=10000000d",
                "cleanup-interval=0s",
                "cleanup-interval=1ns",
                "max-source-code-bytes=0",
                "max-source-code-bytes=1048577"
            })
    void rejectsInvalidConfiguration(String value) {
        runner.withPropertyValues("execution.abuse-protection." + value)
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ExecutionAbuseProtectionProperties.class)
    static class PropertiesConfiguration {}
}
