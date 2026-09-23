package com.tutorplatform.file.infrastructure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enable the scheduler only when the operator explicitly opts in. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "app.file-storage.cleanup", name = "enabled", havingValue = "true")
public class FileCleanupSchedulingConfiguration {}
