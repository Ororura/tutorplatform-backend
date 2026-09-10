package com.tutorplatform.report.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LearningPeriodTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void activePeriodEnforcesCoreInvariants() {
        assertThatThrownBy(() -> active(0, 0, 480))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> active(1, -1, 480))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> active(1, 0, 0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void statusSpecificCompletionValuesAreEnforced() {
        assertThatThrownBy(() -> new LearningPeriod(
            UUID.randomUUID(), UUID.randomUUID(), 1, 0, 480, 480,
            LearningPeriodStatus.ACTIVE, null, NOW, NOW, NOW
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new LearningPeriod(
            UUID.randomUUID(), UUID.randomUUID(), 1, 100, 480, 99,
            LearningPeriodStatus.COMPLETED, NOW, NOW, NOW, NOW
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new LearningPeriod(
            UUID.randomUUID(), UUID.randomUUID(), 1, 0, 480, null,
            LearningPeriodStatus.COMPLETED, NOW, null, NOW, NOW
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void completionKeepsActualOvershoot() {
        LearningPeriod completed = active(1, 0, 480)
            .withStartedAt(NOW, NOW)
            .complete(510, NOW.plusSeconds(3600), NOW.plusSeconds(3600));

        assertThat(completed.status()).isEqualTo(LearningPeriodStatus.COMPLETED);
        assertThat(completed.endCumulativeMinutes()).isEqualTo(510);
        assertThat(completed.completedAt()).isEqualTo(NOW.plusSeconds(3600));
    }

    private LearningPeriod active(int sequence, int start, int target) {
        return LearningPeriod.active(
            UUID.randomUUID(), UUID.randomUUID(), sequence, start, target, NOW
        );
    }
}
