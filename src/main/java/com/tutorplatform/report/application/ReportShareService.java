package com.tutorplatform.report.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.report.api.request.CreateReportShareRequest;
import com.tutorplatform.report.api.response.ReportShareCreatedResponse;
import com.tutorplatform.report.api.response.ReportShareListResponse;
import com.tutorplatform.report.api.response.ReportShareSummaryResponse;
import com.tutorplatform.report.application.exception.InvalidReportShareExpirationException;
import com.tutorplatform.report.application.exception.ReportShareNotAllowedException;
import com.tutorplatform.report.application.exception.ReportShareNotFoundException;
import com.tutorplatform.report.domain.ProgressReport;
import com.tutorplatform.report.domain.ProgressReportStatus;
import com.tutorplatform.report.domain.ReportShare;
import com.tutorplatform.report.domain.ReportShareRepository;
import com.tutorplatform.student.application.invite.StudentInviteTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@Service
public class ReportShareService {

    private final ProgressReportQueryService reportQueryService;
    private final ReportShareRepository reportShareRepository;
    private final StudentInviteTokenService tokenService;
    private final String publicFrontendBaseUrl;

    public ReportShareService(
        ProgressReportQueryService reportQueryService,
        ReportShareRepository reportShareRepository,
        StudentInviteTokenService tokenService,
        @Value("${app.student-invites.public-frontend-base-url}") URI publicFrontendBaseUrl
    ) {
        this.reportQueryService = reportQueryService;
        this.reportShareRepository = reportShareRepository;
        this.tokenService = tokenService;
        this.publicFrontendBaseUrl = stripTrailingSlash(publicFrontendBaseUrl.toString());
    }

    @Transactional
    public ReportShareCreatedResponse create(
        AuthenticatedUser principal,
        UUID reportId,
        CreateReportShareRequest request
    ) {
        ProgressReport report = reportQueryService.get(principal, reportId);
        if (report.status() != ProgressReportStatus.PUBLISHED) {
            throw new ReportShareNotAllowedException();
        }

        Instant now = Instant.now();
        if (request.expiresAt() != null && !request.expiresAt().isAfter(now)) {
            throw new InvalidReportShareExpirationException();
        }

        StudentInviteTokenService.Token token = tokenService.createToken();
        ReportShare share = reportShareRepository.saveAndFlush(new ReportShare(
            UUID.randomUUID(), report.id(), report.generatedByTeacherId(), token.hash(),
            request.expiresAt(), null, null
        ));
        return new ReportShareCreatedResponse(
            share.id(), share.reportId(), share.expiresAt(),
            publicFrontendBaseUrl + "/reports/" + token.rawValue(), share.createdAt()
        );
    }

    @Transactional(readOnly = true)
    public ReportShareListResponse list(AuthenticatedUser principal, UUID reportId) {
        ProgressReport report = reportQueryService.get(principal, reportId);
        Instant now = Instant.now();
        return new ReportShareListResponse(reportShareRepository.listByReportId(report.id()).stream()
            .map(share -> new ReportShareSummaryResponse(
                share.id(), share.reportId(), share.statusAt(now), share.expiresAt(),
                share.revokedAt(), share.createdAt()
            ))
            .toList());
    }

    @Transactional
    public void revoke(AuthenticatedUser principal, UUID reportId, UUID shareId) {
        ProgressReport report = reportQueryService.get(principal, reportId);
        ReportShare share = reportShareRepository.findByIdAndReportId(shareId, report.id())
            .orElseThrow(ReportShareNotFoundException::new);
        if (share.revokedAt() == null) {
            reportShareRepository.saveAndFlush(share.revoke(Instant.now()));
        }
    }

    private static String stripTrailingSlash(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }
}
