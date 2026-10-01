package com.tutorplatform.auth.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class AuthRateLimitPropertiesTest {
    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void bindsSafeDefaults() {
        runner.run(
                context -> {
                    assertThat(context).hasNotFailed();
                    var properties = context.getBean(AuthRateLimitProperties.class);
                    assertThat(properties.loginLimit()).isEqualTo(10);
                    assertThat(properties.loginWindow()).isEqualTo(Duration.ofMinutes(1));
                    assertThat(properties.registrationLimit()).isEqualTo(5);
                    assertThat(properties.registrationWindow()).isEqualTo(Duration.ofMinutes(10));
                    assertThat(properties.studentInvitationLimit()).isEqualTo(10);
                    assertThat(properties.teacherInvitationLimit()).isEqualTo(10);
                    assertThat(properties.invitationWindow()).isEqualTo(Duration.ofMinutes(10));
                    assertThat(properties.maxBuckets()).isEqualTo(10000);
                    assertThat(properties.cleanupInterval()).isEqualTo(Duration.ofSeconds(30));
                });
    }

    @Test
    void bindsOverrides() {
        runner.withPropertyValues(
                        "app.security.auth-rate-limit.login-limit=3",
                        "app.security.auth-rate-limit.login-window=2m",
                        "app.security.auth-rate-limit.max-buckets=50",
                        "app.security.auth-rate-limit.cleanup-interval=5s")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            var properties = context.getBean(AuthRateLimitProperties.class);
                            assertThat(properties.loginLimit()).isEqualTo(3);
                            assertThat(properties.loginWindow()).isEqualTo(Duration.ofMinutes(2));
                            assertThat(properties.maxBuckets()).isEqualTo(50);
                            assertThat(properties.cleanupInterval())
                                    .isEqualTo(Duration.ofSeconds(5));
                        });
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "login-limit=0",
                "registration-limit=-1",
                "student-invitation-limit=0",
                "teacher-invitation-limit=0",
                "max-buckets=0",
                "login-window=0s",
                "registration-window=-1s",
                "invitation-window=0s",
                "cleanup-interval=0s",
                "login-window=10000000d"
            })
    void rejectsUnsafeConfiguration(String value) {
        runner.withPropertyValues("app.security.auth-rate-limit." + value)
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AuthRateLimitProperties.class)
    static class PropertiesConfiguration {}
}
