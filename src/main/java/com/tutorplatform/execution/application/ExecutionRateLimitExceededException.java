package com.tutorplatform.execution.application;

public final class ExecutionRateLimitExceededException extends RuntimeException {
    private final long retryAfterSeconds;

    public ExecutionRateLimitExceededException(long retryAfterSeconds) {
        super("Too many code executions. Try again later.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
