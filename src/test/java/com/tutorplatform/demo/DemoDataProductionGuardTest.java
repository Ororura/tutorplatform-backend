package com.tutorplatform.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemoDataProductionGuardTest {

    @Test
    void productionWithDemoDataEnabledFailsStartup() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("app.demo-data.enabled", "true");
        environment.setActiveProfiles("prod", "demo");

        assertThatThrownBy(() -> new DemoDataProductionGuard().postProcessEnvironment(
            environment, new SpringApplication()
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must never be true with the prod profile");
    }

    @Test
    void productionWithDemoDataDisabledIsSafe() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("app.demo-data.enabled", "false");
        environment.setActiveProfiles("prod");

        assertThatCode(() -> new DemoDataProductionGuard().postProcessEnvironment(
            environment, new SpringApplication()
        )).doesNotThrowAnyException();
    }
}
