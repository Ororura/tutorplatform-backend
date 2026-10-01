package com.tutorplatform.auth.infrastructure.ratelimit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthRateLimitProperties.class)
@EnableScheduling
public class AuthRateLimitConfiguration {
    @Bean
    AuthRateLimiter authRateLimiter(AuthRateLimitProperties properties) {
        return new AuthRateLimiter(properties);
    }
}
