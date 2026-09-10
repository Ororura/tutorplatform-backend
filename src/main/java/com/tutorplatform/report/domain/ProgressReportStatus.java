package com.tutorplatform.report.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProgressReportStatus")
public enum ProgressReportStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
