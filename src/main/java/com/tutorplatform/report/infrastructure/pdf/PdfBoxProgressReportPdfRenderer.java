package com.tutorplatform.report.infrastructure.pdf;

import com.tutorplatform.report.application.ProgressReportPdfModel;
import com.tutorplatform.report.application.ProgressReportPdfRenderer;
import com.tutorplatform.report.application.exception.ProgressReportPdfRenderException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class PdfBoxProgressReportPdfRenderer implements ProgressReportPdfRenderer {

    private static final float MARGIN = 48;
    private static final float BODY_SIZE = 10.5f;
    private static final float LINE_HEIGHT = 15;
    private static final String FONT_RESOURCE = "fonts/NotoSans-Regular.ttf";

    private final ZoneId presentationZone;
    private final DateTimeFormatter dateFormatter;

    public PdfBoxProgressReportPdfRenderer(
            @Value("${app.reports.pdf.presentation-zone:Europe/Moscow}") String presentationZone) {
        this.presentationZone = ZoneId.of(presentationZone);
        this.dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    }

    @Override
    public byte[] render(ProgressReportPdfModel model) {
        try (PDDocument document = new PDDocument();
                var fontStream = new ClassPathResource(FONT_RESOURCE).getInputStream();
                var output = new ByteArrayOutputStream()) {
            PDFont font = PDType0Font.load(document, fontStream, true);
            try (Writer writer = new Writer(document, font)) {
                writer.title("Отчёт о прогрессе");
                writer.paragraph("Период: " + formatPeriod(model));

                writer.section("1. Общая статистика");
                writer.keyValue("Время обучения", formatMinutes(model.learningMinutes()));
                writer.keyValue(
                        "Количество занятий", Long.toString(model.metrics().sessionsCount()));
                writer.keyValue(
                        "Посещаемость", Math.round(model.metrics().attendanceRate() * 100) + "%");
                writer.keyValue(
                        "Домашние задания",
                        fraction(
                                model.metrics().homeworkCompleted(),
                                model.metrics().homeworkAssigned()));
                writer.keyValue(
                        "Практические задания",
                        fraction(
                                model.metrics().practiceCompleted(),
                                model.metrics().practiceAssigned()));

                writer.section("2. Оценка преподавателя");
                writer.keyValue("Понимание", assessment(model.assessment().understandingAverage()));
                writer.keyValue(
                        "Самостоятельность", assessment(model.assessment().independenceAverage()));
                writer.keyValue("Практика", assessment(model.assessment().practiceAverage()));
                writer.keyValue(
                        "Домашняя работа", assessment(model.assessment().homeworkAverage()));

                writer.section("3. Темы");
                writer.subheading("Завершённые");
                writer.list(
                        model.completedTopics().stream()
                                .map(ProgressReportPdfModel.Topic::title)
                                .toList());
                writer.subheading("В процессе");
                writer.list(
                        model.inProgressTopics().stream()
                                .map(ProgressReportPdfModel.Topic::title)
                                .toList());

                if (!model.skills().isEmpty()) {
                    writer.subheading("Навыки");
                    writer.list(
                            model.skills().stream()
                                    .map(
                                            skill ->
                                                    skill.name()
                                                            + ": "
                                                            + assessment(skill.progress()))
                                    .toList());
                }

                writer.section("4. Комментарий преподавателя");
                writer.paragraph(orMissing(model.teacherSummary()));

                writer.section("5. План следующего периода");
                writer.paragraph(orMissing(model.nextPeriodPlan()));
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new ProgressReportPdfRenderException("pdf_io", exception);
        } catch (IllegalArgumentException exception) {
            throw new ProgressReportPdfRenderException("unsupported_text", exception);
        }
    }

    private String formatPeriod(ProgressReportPdfModel model) {
        return dateFormatter.format(model.periodStartedAt().atZone(presentationZone))
                + " - "
                + dateFormatter.format(model.periodEndedAt().atZone(presentationZone));
    }

    private static String formatMinutes(int minutes) {
        int hours = minutes / 60;
        int remainder = minutes % 60;
        if (hours == 0) {
            return remainder + " мин";
        }
        return hours + " ч " + remainder + " мин (" + minutes + " мин)";
    }

    private static String fraction(long completed, long assigned) {
        return completed + " / " + assigned;
    }

    private static String assessment(BigDecimal value) {
        return value == null
                ? "Нет оценки"
                : value.setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    private static String orMissing(String value) {
        return value == null || value.isBlank() ? "Не указано" : value;
    }

    private static final class Writer implements AutoCloseable {
        private final PDDocument document;
        private final PDFont font;
        private PDPageContentStream content;
        private float y;

        private Writer(PDDocument document, PDFont font) throws IOException {
            this.document = document;
            this.font = font;
            newPage();
        }

        private void title(String text) throws IOException {
            ensure(32);
            line(text, 20, MARGIN, 25);
            y -= 10;
        }

        private void section(String text) throws IOException {
            ensure(36);
            y -= 12;
            line(text, 13, MARGIN, 19);
            y -= 3;
        }

        private void subheading(String text) throws IOException {
            ensure(24);
            y -= 5;
            line(text, 11.5f, MARGIN, 17);
        }

        private void keyValue(String key, String value) throws IOException {
            paragraph(key + ": " + value);
        }

        private void paragraph(String text) throws IOException {
            String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
            String[] paragraphs = normalized.split("\n", -1);
            for (int index = 0; index < paragraphs.length; index++) {
                if (paragraphs[index].isEmpty()) {
                    ensure(LINE_HEIGHT);
                    y -= LINE_HEIGHT;
                } else {
                    for (String line : wrap(paragraphs[index], availableWidth(MARGIN), BODY_SIZE)) {
                        ensure(LINE_HEIGHT);
                        line(line, BODY_SIZE, MARGIN, LINE_HEIGHT);
                    }
                }
                if (index < paragraphs.length - 1) {
                    y -= 2;
                }
            }
        }

        private void list(List<String> items) throws IOException {
            if (items.isEmpty()) {
                paragraph("Нет");
                return;
            }
            float indent = MARGIN + 14;
            for (String item : items) {
                List<String> lines = wrap(item, availableWidth(indent), BODY_SIZE);
                for (int index = 0; index < lines.size(); index++) {
                    ensure(LINE_HEIGHT);
                    line(
                            (index == 0 ? "• " : "") + lines.get(index),
                            BODY_SIZE,
                            index == 0 ? MARGIN : indent,
                            LINE_HEIGHT);
                }
            }
        }

        private List<String> wrap(String text, float maxWidth, float fontSize) throws IOException {
            List<String> lines = new ArrayList<>();
            String remaining = text.strip();
            if (remaining.isEmpty()) {
                return List.of("");
            }
            while (!remaining.isEmpty()) {
                int split = fittingEnd(remaining, maxWidth, fontSize);
                if (split == remaining.length()) {
                    lines.add(remaining);
                    break;
                }
                int whitespace = remaining.lastIndexOf(' ', split - 1);
                if (whitespace > 0) {
                    split = whitespace;
                }
                lines.add(remaining.substring(0, split).stripTrailing());
                remaining = remaining.substring(split).stripLeading();
            }
            return lines;
        }

        private int fittingEnd(String text, float maxWidth, float fontSize) throws IOException {
            int end = 0;
            while (end < text.length()) {
                int next = text.offsetByCodePoints(end, 1);
                if (width(text.substring(0, next), fontSize) > maxWidth) {
                    return end == 0 ? next : end;
                }
                end = next;
            }
            return end;
        }

        private float width(String text, float fontSize) throws IOException {
            return font.getStringWidth(text) / 1000 * fontSize;
        }

        private float availableWidth(float x) {
            return PDRectangle.A4.getWidth() - MARGIN - x;
        }

        private void line(String text, float size, float x, float leading) throws IOException {
            content.beginText();
            content.setFont(font, size);
            content.newLineAtOffset(x, y);
            content.showText(text);
            content.endText();
            y -= leading;
        }

        private void ensure(float height) throws IOException {
            if (y - height < MARGIN) {
                content.close();
                newPage();
            }
        }

        private void newPage() throws IOException {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = PDRectangle.A4.getHeight() - MARGIN;
        }

        @Override
        public void close() throws IOException {
            content.close();
        }
    }
}
