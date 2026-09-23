package com.tutorplatform.file.infrastructure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enable read-only S3 inventory checks without enabling file deletion jobs. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "app.file-storage", name = "provider", havingValue = "S3")
@ConditionalOnProperty(
        prefix = "app.file-storage.s3-reconciliation",
        name = "enabled",
        havingValue = "true")
public class S3AuditSchedulingConfiguration {}
