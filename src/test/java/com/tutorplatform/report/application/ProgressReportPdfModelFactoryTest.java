package com.tutorplatform.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tutorplatform.report.application.exception.ProgressReportPdfNotAvailableException;
import com.tutorplatform.report.domain.ProgressReport;
import com.tutorplatform.report.domain.ProgressReportSnapshotSchemas;
import com.tutorplatform.report.domain.ProgressReportSnapshotV1;
import com.tutorplatform.report.domain.ProgressReportStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProgressReportPdfModelFactoryTest {

    private final ProgressReportPdfModelFactory factory = new ProgressReportPdfModelFactory();

    @Test
    void mapsOnlyThePersistedSnapshotAndPublishedNarrativeToThePdfModel() {
        ProgressReport report = report(ProgressReportStatus.PUBLISHED);

        ProgressReportPdfModel model = factory.fromPublished(report);

        assertThat(model.periodStartedAt()).isEqualTo(report.periodStartedAt());
        assertThat(model.periodEndedAt()).isEqualTo(report.periodEndedAt());
        assertThat(model.learningMinutes()).isEqualTo(510);
        assertThat(model.metrics())
                .isEqualTo(new ProgressReportPdfModel.Metrics(510, 9, 0.75, 7, 6, 12, 10));
        assertThat(model.assessment().understandingAverage()).isEqualByComparingTo("4.25");
        assertThat(model.assessment().independenceAverage()).isNull();
        assertThat(model.completedTopics())
                .extracting(ProgressReportPdfModel.Topic::title)
                .containsExactly("Дроби");
        assertThat(model.inProgressTopics())
                .extracting(ProgressReportPdfModel.Topic::title)
                .containsExactly("Linear equations");
        assertThat(model.skills())
                .extracting(ProgressReportPdfModel.Skill::name)
                .containsExactly("Самостоятельность");
        assertThat(model.teacherSummary()).isEqualTo("Хороший прогресс\nKeep going");
        assertThat(model.nextPeriodPlan()).isEqualTo("Повторить материал");
    }

    @Test
    void rejectsDraftsBeforeTheyReachTheRenderer() {
        assertThatThrownBy(() -> factory.fromPublished(report(ProgressReportStatus.DRAFT)))
                .isInstanceOf(ProgressReportPdfNotAvailableException.class);
    }

    private ProgressReport report(ProgressReportStatus status) {
        Instant now = Instant.parse("2026-02-01T12:00:00Z");
        return new ProgressReport(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                status,
                Instant.parse("2026-01-01T21:30:00Z"),
                Instant.parse("2026-01-31T21:30:00Z"),
                510,
                ProgressReportSnapshotSchemas.V1,
                new ProgressReportSnapshotV1(
                        new ProgressReportSnapshotV1.Metrics(510, 9, 0.75, 7, 6, 12, 10),
                        new ProgressReportSnapshotV1.Assessment(
                                new BigDecimal("4.25"),
                                null,
                                new BigDecimal("4.0"),
                                new BigDecimal("3.5")),
                        new ProgressReportSnapshotV1.Topics(
                                List.of(
                                        new ProgressReportSnapshotV1.Topic(
                                                UUID.randomUUID(), "Дроби")),
                                List.of(
                                        new ProgressReportSnapshotV1.Topic(
                                                UUID.randomUUID(), "Linear equations"))),
                        List.of(
                                new ProgressReportSnapshotV1.Skill(
                                        UUID.randomUUID(),
                                        "Самостоятельность",
                                        new BigDecimal("0.8")))),
                "Хороший прогресс\nKeep going",
                "Повторить материал",
                status == ProgressReportStatus.PUBLISHED ? now : null,
                1,
                now.minusSeconds(60),
                now);
    }
}
