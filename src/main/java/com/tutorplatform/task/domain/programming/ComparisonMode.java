package com.tutorplatform.task.domain.programming;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "EXACT compares output verbatim. NORMALIZED will normalize line endings and trailing whitespace during execution.", enumAsRef = true)
public enum ComparisonMode {
    EXACT,
    NORMALIZED
}
