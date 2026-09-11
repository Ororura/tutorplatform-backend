package com.tutorplatform.report.infrastructure.pdf;

import com.tutorplatform.report.application.ProgressReportPdfModel;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfBoxProgressReportPdfRendererTest {

    private final PdfBoxProgressReportPdfRenderer renderer =
        new PdfBoxProgressReportPdfRenderer("Europe/Moscow");

    @Test
    void rendersAReadableTextPdfWithCyrillicLatinNumbersAndMultilineText() throws Exception {
        byte[] pdf = renderer.render(model(
            List.of(new ProgressReportPdfModel.Topic("Очень длинное название темы ".repeat(18))),
            List.of(new ProgressReportPdfModel.Topic("Linear equations 123")),
            "Первая строка\nВторая строка with Latin 42",
            "Следующий период"
        ));

        assertThat(pdf).isNotEmpty().startsWith("%PDF-".getBytes());
        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            assertThat(text).contains(
                "Отчёт о прогрессе", "8 ч 30 мин (510 мин)", "75%",
                "Нет оценки", "Очень длинное название темы", "Linear equations 123",
                "Первая строка", "Вторая строка with Latin 42", "Следующий период"
            );
        }
    }

    @Test
    void rendersEmptyTopicsAndNullOptionalNarrative() throws Exception {
        byte[] pdf = renderer.render(model(List.of(), List.of(), null, null));

        try (var document = Loader.loadPDF(pdf)) {
            assertThat(new PDFTextStripper().getText(document))
                .contains("Завершённые", "В процессе", "Нет", "Не указано");
        }
    }

    private ProgressReportPdfModel model(
        List<ProgressReportPdfModel.Topic> completed,
        List<ProgressReportPdfModel.Topic> inProgress,
        String summary,
        String plan
    ) {
        return new ProgressReportPdfModel(
            Instant.parse("2026-01-01T21:30:00Z"),
            Instant.parse("2026-01-31T21:30:00Z"),
            510,
            new ProgressReportPdfModel.Metrics(510, 9, 0.75, 7, 6, 12, 10),
            new ProgressReportPdfModel.Assessment(
                new BigDecimal("4.25"), null, new BigDecimal("4.0"), new BigDecimal("3.5")
            ),
            completed,
            inProgress,
            List.of(),
            summary,
            plan
        );
    }
}
