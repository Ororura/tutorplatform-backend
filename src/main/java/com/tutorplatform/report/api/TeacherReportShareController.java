package com.tutorplatform.report.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.report.api.request.CreateReportShareRequest;
import com.tutorplatform.report.api.response.ReportShareCreatedResponse;
import com.tutorplatform.report.api.response.ReportShareListResponse;
import com.tutorplatform.report.application.ReportShareService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher/reports/{reportId}/shares")
public class TeacherReportShareController implements TeacherReportShareApi {

    private final ReportShareService reportShareService;

    public TeacherReportShareController(ReportShareService reportShareService) {
        this.reportShareService = reportShareService;
    }

    @Override
    @PostMapping
    public ResponseEntity<ReportShareCreatedResponse> createReportShare(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID reportId,
            @Valid @RequestBody CreateReportShareRequest request) {
        ReportShareCreatedResponse response =
                reportShareService.create(principal, reportId, request);
        return ResponseEntity.created(
                        URI.create(
                                "/api/v1/teacher/reports/" + reportId + "/shares/" + response.id()))
                .body(response);
    }

    @Override
    @GetMapping
    public ReportShareListResponse listReportShares(
            @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID reportId) {
        return reportShareService.list(principal, reportId);
    }

    @Override
    @DeleteMapping("/{shareId}")
    public ResponseEntity<Void> revokeReportShare(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID reportId,
            @PathVariable UUID shareId) {
        reportShareService.revoke(principal, reportId, shareId);
        return ResponseEntity.noContent().build();
    }
}
