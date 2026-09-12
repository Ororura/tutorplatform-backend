package com.tutorplatform.config;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionInfrastructureConfigurationTest {

    @Test
    void rejectsBlankDatabasePassword() {
        assertThatThrownBy(() -> configuration("", "S3", URI.create("https://learn.example.com")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("spring.datasource.password");
    }

    @Test
    void rejectsLocalFileStorageInProduction() {
        assertThatThrownBy(() -> configuration("secret", "LOCAL", URI.create("https://learn.example.com")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("app.file-storage.provider must be S3");
    }

    @Test
    void rejectsRelativePublicFrontendUrl() {
        assertThatThrownBy(() -> configuration("secret", "S3", URI.create("/relative")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("public-frontend-base-url");
    }

    private static ProductionInfrastructureConfiguration configuration(
        String password,
        String provider,
        URI publicUrl
    ) {
        return new ProductionInfrastructureConfiguration(
            "jdbc:postgresql://database:5432/tutor", "tutor", password, provider, publicUrl
        );
    }
}
