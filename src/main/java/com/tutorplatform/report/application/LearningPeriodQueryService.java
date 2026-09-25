package com.tutorplatform.report.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.report.domain.LearningPeriod;
import com.tutorplatform.report.domain.LearningPeriodRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LearningPeriodQueryService {

    private final ProgressReportAuthorization authorization;
    private final LearningPeriodRepository learningPeriodRepository;
    private final ProgressReportReadQuery progressReportReadQuery;

    LearningPeriodQueryService(
            ProgressReportAuthorization authorization,
            LearningPeriodRepository learningPeriodRepository,
            ProgressReportReadQuery progressReportReadQuery) {
        this.authorization = authorization;
        this.learningPeriodRepository = learningPeriodRepository;
        this.progressReportReadQuery = progressReportReadQuery;
    }

    public List<LearningPeriodSummary> list(
            AuthenticatedUser principal, UUID studentId, UUID studentProgramId) {
        authorization.require(principal, studentId, studentProgramId);
        List<LearningPeriod> periods =
                learningPeriodRepository.findAllByStudentProgramIdOrderBySequenceNo(
                        studentProgramId);
        Map<UUID, UUID> reportIds =
                progressReportReadQuery.findIdsByLearningPeriodIds(
                        periods.stream().map(LearningPeriod::id).toList());
        return periods.stream()
                .map(period -> LearningPeriodSummary.from(period, reportIds.get(period.id())))
                .toList();
    }
}
