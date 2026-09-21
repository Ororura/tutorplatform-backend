package com.tutorplatform.shared.time;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class InstantPrecision {

    private InstantPrecision() {}

    public static Instant database(Instant instant) {
        return instant == null ? null : instant.truncatedTo(ChronoUnit.MICROS);
    }
}
