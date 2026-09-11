package com.tutorplatform.report.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReportShareStatus", enumAsRef = true)
public enum ReportShareStatus {
    ACTIVE,
    EXPIRED,
    REVOKED
}
