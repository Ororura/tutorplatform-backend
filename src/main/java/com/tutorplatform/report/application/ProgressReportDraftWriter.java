package com.tutorplatform.report.application;

import com.tutorplatform.report.application.exception.ProgressReportConflictException;
import com.tutorplatform.report.domain.ProgressReport;
import com.tutorplatform.report.domain.ProgressReportRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ProgressReportDraftWriter {

    private final ProgressReportRepository repository;

    ProgressReportDraftWriter(ProgressReportRepository repository) {
        this.repository = repository;
    }

    @Transactional
    ProgressReport save(ProgressReport report) {
        try {
            return repository.saveAndFlush(report);
        } catch (DataIntegrityViolationException exception) {
            throw new ProgressReportConflictException(exception);
        }
    }
}
