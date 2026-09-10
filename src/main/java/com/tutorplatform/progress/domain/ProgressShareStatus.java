package com.tutorplatform.progress.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum ProgressShareStatus {
    ACTIVE,
    EXPIRED,
    REVOKED
}
