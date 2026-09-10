package com.tutorplatform.progress.application;

import java.time.Instant;
import java.util.Objects;

/**
 * A closed event-time interval. Both boundaries are included.
 */
public record ProgressInterval(Instant startedAt, Instant endedAt) {

    public ProgressInterval {
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(endedAt, "endedAt");
        if (endedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("endedAt must not precede startedAt");
        }
    }
}
