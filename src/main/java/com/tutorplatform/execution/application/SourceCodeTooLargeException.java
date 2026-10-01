package com.tutorplatform.execution.application;

public final class SourceCodeTooLargeException extends RuntimeException {
    public SourceCodeTooLargeException(int maxBytes) {
        super("sourceCode must not exceed " + maxBytes + " UTF-8 bytes");
    }
}
