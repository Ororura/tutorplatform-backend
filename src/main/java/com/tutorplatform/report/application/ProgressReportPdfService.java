package com.tutorplatform.report.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.report.application.exception.ProgressReportPdfRenderException;
import com.tutorplatform.report.domain.ProgressReport;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgressReportPdfService {

    private static final Logger log = LoggerFactory.getLogger(ProgressReportPdfService.class);

    private final ProgressReportQueryService queryService;
    private final PublicProgressReportService publicProgressReportService;
    private final ProgressReportPdfModelFactory modelFactory;
    private final ProgressReportPdfRenderer renderer;
    private final int maxInputCharacters;
    private final int maxOutputBytes;

    public ProgressReportPdfService(
            ProgressReportQueryService queryService,
            PublicProgressReportService publicProgressReportService,
            ProgressReportPdfModelFactory modelFactory,
            ProgressReportPdfRenderer renderer,
            @Value("${app.reports.pdf.max-input-characters:200000}") int maxInputCharacters,
            @Value("${app.reports.pdf.max-output-bytes:5242880}") int maxOutputBytes) {
        this.queryService = queryService;
        this.publicProgressReportService = publicProgressReportService;
        this.modelFactory = modelFactory;
        this.renderer = renderer;
        this.maxInputCharacters = maxInputCharacters;
        this.maxOutputBytes = maxOutputBytes;
    }

    @Transactional(readOnly = true)
    public ProgressReportPdfDownload downloadForTeacher(
            AuthenticatedUser principal, UUID reportId) {
        return render(queryService.get(principal, reportId));
    }

    @Transactional(readOnly = true)
    public ProgressReportPdfDownload downloadPublic(String rawToken) {
        return render(publicProgressReportService.resolvePublished(rawToken));
    }

    private ProgressReportPdfDownload render(ProgressReport report) {
        ProgressReportPdfModel model = modelFactory.fromPublished(report);
        try {
            enforceInputLimit(model);
            byte[] content = renderer.render(model);
            if (content.length == 0 || content.length > maxOutputBytes) {
                throw new ProgressReportPdfRenderException("output_limit");
            }
            return new ProgressReportPdfDownload(
                    "progress-report-" + report.id() + ".pdf", content);
        } catch (ProgressReportPdfRenderException exception) {
            log.warn(
                    "Progress report PDF generation failed: reportId={}, traceId={}, category={}",
                    report.id(),
                    MDC.get("traceId"),
                    exception.category());
            throw exception;
        } catch (RuntimeException exception) {
            log.warn(
                    "Progress report PDF generation failed: reportId={}, traceId={}, category=renderer",
                    report.id(),
                    MDC.get("traceId"));
            throw new ProgressReportPdfRenderException("renderer", exception);
        }
    }

    private void enforceInputLimit(ProgressReportPdfModel model) {
        long characters = length(model.teacherSummary()) + length(model.nextPeriodPlan());
        characters +=
                model.completedTopics().stream().mapToLong(topic -> length(topic.title())).sum();
        characters +=
                model.inProgressTopics().stream().mapToLong(topic -> length(topic.title())).sum();
        characters += model.skills().stream().mapToLong(skill -> length(skill.name())).sum();
        if (characters > maxInputCharacters) {
            throw new ProgressReportPdfRenderException("input_limit");
        }
    }

    private static int length(String value) {
        return value == null ? 0 : value.length();
    }
}
