package com.tutorplatform.demo;

import java.util.Arrays;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;

public class DemoDataProductionGuard implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment, SpringApplication application) {
        boolean production = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (production && environment.getProperty("app.demo-data.enabled", Boolean.class, false)) {
            throw new IllegalStateException(
                    "Refusing to start: app.demo-data.enabled must never be true with the prod profile");
        }
    }
}
