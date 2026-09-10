package com.tutorplatform.progress.application;

import com.tutorplatform.program.application.ProgramQuery;
import com.tutorplatform.progress.application.exception.ProgressStudentProgramNotFoundException;
import com.tutorplatform.progress.domain.CurrentProgress;
import com.tutorplatform.progress.domain.ProgressCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetCurrentProgressService {

    private final ProgramQuery programQuery;
    private final ProgressReadRepository progressReadRepository;
    private final ProgressCalculator progressCalculator;

    public GetCurrentProgressService(
        ProgramQuery programQuery,
        ProgressReadRepository progressReadRepository,
        ProgressCalculator progressCalculator
    ) {
        this.programQuery = programQuery;
        this.progressReadRepository = progressReadRepository;
        this.progressCalculator = progressCalculator;
    }

    public CurrentProgress getCurrentProgress(UUID studentProgramId) {
        programQuery.findStudentProgram(studentProgramId)
            .orElseThrow(ProgressStudentProgramNotFoundException::new);

        return progressCalculator.calculate(
            studentProgramId,
            progressReadRepository.getSessionMetrics(studentProgramId),
            progressReadRepository.findTopicProgress(studentProgramId),
            progressReadRepository.getHomeworkMetrics(studentProgramId),
            progressReadRepository.getPracticeMetrics(studentProgramId),
            progressReadRepository.getAssessmentAverages(studentProgramId)
        );
    }
}
