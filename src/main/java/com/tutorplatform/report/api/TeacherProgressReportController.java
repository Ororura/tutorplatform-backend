package com.tutorplatform.report.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.report.api.request.CreateProgressReportRequest;
import com.tutorplatform.report.api.request.PublishProgressReportRequest;
import com.tutorplatform.report.api.request.UpdateProgressReportRequest;
import com.tutorplatform.report.api.response.ProgressReportDetailsResponse;
import com.tutorplatform.report.api.response.ProgressReportPageResponse;
import com.tutorplatform.report.application.*;
import com.tutorplatform.report.domain.ProgressReport;
import com.tutorplatform.report.domain.ProgressReportStatus;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher/reports")
public class TeacherProgressReportController implements TeacherProgressReportApi {

    private final CreateProgressReportDraft createDraft;
    private final ProgressReportQueryService queryService;
    private final EditProgressReportDraft editDraft;
    private final PublishProgressReport publishReport;
    private final ProgressReportPdfService pdfService;

    public TeacherProgressReportController(
        CreateProgressReportDraft createDraft,
        ProgressReportQueryService queryService,
        EditProgressReportDraft editDraft,
        PublishProgressReport publishReport,
        ProgressReportPdfService pdfService
    ) {
        this.createDraft = createDraft;
        this.queryService = queryService;
        this.editDraft = editDraft;
        this.publishReport = publishReport;
        this.pdfService = pdfService;
    }

    @Override
    @PostMapping
    public ResponseEntity<ProgressReportDetailsResponse> createProgressReport(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @Valid @RequestBody CreateProgressReportRequest request
    ) {
        ProgressReport report = createDraft.create(
            principal, request.studentProgramId(), request.learningPeriodId()
        );
        return ResponseEntity.created(
            URI.create("/api/v1/teacher/reports/" + report.id())
        ).body(ProgressReportDetailsResponse.from(report));
    }

    @Override
    @GetMapping
    public ProgressReportPageResponse listProgressReports(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @RequestParam(required = false) UUID studentProgramId,
        @RequestParam(required = false) ProgressReportStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return ProgressReportPageResponse.from(queryService.list(
            principal, studentProgramId, status, page, size, sort
        ));
    }

    @Override
    @GetMapping("/{reportId}")
    public ProgressReportDetailsResponse getProgressReport(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID reportId
    ) {
        return ProgressReportDetailsResponse.from(queryService.get(principal, reportId));
    }

    @Override
    @GetMapping("/{reportId}/pdf")
    public ResponseEntity<byte[]> downloadProgressReportPdf(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID reportId
    ) {
        ProgressReportPdfDownload download = pdfService.downloadForTeacher(principal, reportId);
        return ProgressReportPdfResponse.attachment(download);
    }

    @Override
    @PatchMapping("/{reportId}")
    public ProgressReportDetailsResponse updateProgressReport(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID reportId,
        @Valid @RequestBody UpdateProgressReportRequest request
    ) {
        return ProgressReportDetailsResponse.from(editDraft.edit(
            principal,
            reportId,
            new EditProgressReportDraftCommand(
                request.getTeacherSummary(),
                request.getNextPeriodPlan(),
                request.getVersion(),
                request.isTeacherSummaryPresent(),
                request.isNextPeriodPlanPresent()
            )
        ));
    }

    @Override
    @PostMapping("/{reportId}/publish")
    public ProgressReportDetailsResponse publishProgressReport(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID reportId,
        @Valid @RequestBody PublishProgressReportRequest request
    ) {
        return ProgressReportDetailsResponse.from(
            publishReport.publish(principal, reportId, request.version())
        );
    }

}
