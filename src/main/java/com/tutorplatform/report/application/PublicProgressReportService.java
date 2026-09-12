package com.tutorplatform.report.application;

import com.tutorplatform.report.api.response.PublicProgressReportResponse;
import com.tutorplatform.report.application.exception.ReportShareExpiredException;
import com.tutorplatform.report.application.exception.ReportShareNotFoundException;
import com.tutorplatform.report.application.exception.ReportShareRevokedException;
import com.tutorplatform.report.domain.ProgressReport;
import com.tutorplatform.report.domain.ProgressReportRepository;
import com.tutorplatform.report.domain.ProgressReportStatus;
import com.tutorplatform.report.domain.ReportShare;
import com.tutorplatform.report.domain.ReportShareRepository;
import com.tutorplatform.student.application.invite.StudentInviteTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class PublicProgressReportService {

    private final StudentInviteTokenService tokenService;
    private final ReportShareRepository reportShareRepository;
    private final ProgressReportRepository progressReportRepository;

    public PublicProgressReportService(
        StudentInviteTokenService tokenService,
        ReportShareRepository reportShareRepository,
        ProgressReportRepository progressReportRepository
    ) {
        this.tokenService = tokenService;
        this.reportShareRepository = reportShareRepository;
        this.progressReportRepository = progressReportRepository;
    }

    @Transactional(readOnly = true)
    public PublicProgressReportResponse get(String rawToken) {
        return PublicProgressReportResponse.from(resolvePublished(rawToken));
    }

    @Transactional(readOnly = true)
    public ProgressReport resolvePublished(String rawToken) {
        ReportShare share = reportShareRepository.findByTokenHash(tokenService.hash(rawToken))
            .orElseThrow(ReportShareNotFoundException::new);
        validateShare(share, Instant.now());
        return progressReportRepository.findById(share.reportId())
            .filter(candidate -> candidate.status() == ProgressReportStatus.PUBLISHED)
            .orElseThrow(ReportShareNotFoundException::new);
    }

    private void validateShare(ReportShare share, Instant now) {
        if (share.revokedAt() != null) {
            throw new ReportShareRevokedException();
        }
        if (share.expiresAt() != null && share.expiresAt().isBefore(now)) {
            throw new ReportShareExpiredException();
        }
    }
}
