package com.tutorplatform.execution.infrastructure.http;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ExecutionProperties.class)
class ExecutionHttpConfiguration {}
