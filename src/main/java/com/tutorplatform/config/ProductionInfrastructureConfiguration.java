package com.tutorplatform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.net.URI;

@Configuration(proxyBeanMethods = false)
@Profile("prod")
class ProductionInfrastructureConfiguration {

    ProductionInfrastructureConfiguration(
        @Value("${spring.datasource.url}") String datasourceUrl,
        @Value("${spring.datasource.username}") String datasourceUsername,
        @Value("${spring.datasource.password}") String datasourcePassword,
        @Value("${app.file-storage.provider}") String fileStorageProvider,
        @Value("${app.student-invites.public-frontend-base-url}") URI publicFrontendBaseUrl
    ) {
        requireText(datasourceUrl, "spring.datasource.url");
        requireText(datasourceUsername, "spring.datasource.username");
        requireText(datasourcePassword, "spring.datasource.password");
        if (!"S3".equalsIgnoreCase(requireText(fileStorageProvider, "app.file-storage.provider"))) {
            throw new IllegalArgumentException("app.file-storage.provider must be S3 in production");
        }
        requireAbsoluteHttpUrl(publicFrontendBaseUrl, "app.student-invites.public-frontend-base-url");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank in production");
        }
        return value;
    }

    private static void requireAbsoluteHttpUrl(URI value, String name) {
        if (value == null || !value.isAbsolute() || value.getHost() == null
            || !("http".equals(value.getScheme()) || "https".equals(value.getScheme()))) {
            throw new IllegalArgumentException(name + " must be an absolute HTTP(S) URL");
        }
    }
}
