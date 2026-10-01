package com.tutorplatform.execution.infrastructure.protection;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ExecutionAbuseProtectionProperties.class)
@EnableScheduling
public class ExecutionAbuseProtectionConfiguration {}
