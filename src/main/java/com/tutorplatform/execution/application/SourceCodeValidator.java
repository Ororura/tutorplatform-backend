package com.tutorplatform.execution.application;

import com.tutorplatform.execution.infrastructure.protection.ExecutionAbuseProtectionProperties;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component
public final class SourceCodeValidator {
    private final ExecutionAbuseProtectionProperties properties;

    public SourceCodeValidator(ExecutionAbuseProtectionProperties properties) {
        this.properties = properties;
    }

    /** Validate before querying context, consuming quota, persisting, or invoking execution. */
    public void validate(String sourceCode) {
        int maxBytes = properties.maxSourceCodeBytes();
        // The length check bounds the temporary UTF-8 allocation even for very large input.
        if (sourceCode != null
                && (sourceCode.length() > maxBytes
                        || sourceCode.getBytes(StandardCharsets.UTF_8).length > maxBytes)) {
            throw new SourceCodeTooLargeException(maxBytes);
        }
    }
}
