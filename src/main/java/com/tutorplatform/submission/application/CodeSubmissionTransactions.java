package com.tutorplatform.submission.application;

import com.tutorplatform.submission.application.exception.SubmissionNotFoundException;
import com.tutorplatform.submission.domain.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Short persistence transactions used around, never across, isolated code execution. */
@Service
public class CodeSubmissionTransactions {

    private final SubmissionRepository submissionRepository;
    private final CodeSubmissionRepository codeSubmissionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CodeSubmissionTransactions(
            SubmissionRepository submissionRepository,
            CodeSubmissionRepository codeSubmissionRepository,
            ApplicationEventPublisher eventPublisher) {
        this.submissionRepository = submissionRepository;
        this.codeSubmissionRepository = codeSubmissionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SubmissionResult createPending(
            UUID studentId,
            UUID studentProgramId,
            UUID taskId,
            UUID homeworkItemId,
            String sourceCode,
            int totalTests) {
        SubmissionAttemptContext context =
                new SubmissionAttemptContext(studentId, studentProgramId, taskId, homeworkItemId);
        SubmissionEntity submission =
                submissionRepository.saveAndFlush(
                        new SubmissionEntity(
                                UUID.randomUUID(),
                                studentId,
                                studentProgramId,
                                taskId,
                                homeworkItemId,
                                submissionRepository.nextAttemptNo(context),
                                SubmissionStatus.SUBMITTED,
                                null,
                                Instant.now()));
        CodeSubmissionEntity code =
                codeSubmissionRepository.saveAndFlush(
                        new CodeSubmissionEntity(submission.getId(), sourceCode, totalTests));
        return SubmissionResult.from(submission).withCodeSubmission(code, true);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SubmissionResult finish(
            UUID submissionId,
            CodeExecutionStatus executionStatus,
            int passedTests,
            int totalTests,
            long executionTimeMs,
            String stdoutExcerpt,
            String stderrExcerpt) {
        SubmissionEntity submission =
                submissionRepository
                        .findById(submissionId)
                        .orElseThrow(SubmissionNotFoundException::new);
        CodeSubmissionEntity code =
                codeSubmissionRepository
                        .findBySubmissionId(submissionId)
                        .orElseThrow(SubmissionNotFoundException::new);
        SubmissionStatus submissionStatus =
                switch (executionStatus) {
                    case PASSED -> SubmissionStatus.PASSED;
                    case SYSTEM_ERROR -> SubmissionStatus.SYSTEM_ERROR;
                    case FAILED, TIMEOUT, RUNTIME_ERROR -> SubmissionStatus.FAILED;
                    case PENDING, RUNNING ->
                            throw new IllegalArgumentException(
                                    "A final execution status is required");
                };
        submission.finishCodeExecution(submissionStatus);
        code.complete(
                executionStatus,
                passedTests,
                totalTests,
                executionTimeMs,
                stdoutExcerpt,
                stderrExcerpt);
        SubmissionEntity savedSubmission = submissionRepository.saveAndFlush(submission);
        CodeSubmissionEntity savedCode = codeSubmissionRepository.saveAndFlush(code);
        eventPublisher.publishEvent(
                new SubmissionReviewedEvent(
                        savedSubmission.getId(), savedSubmission.getStudentId(),
                        savedSubmission.getStudentProgramId(), savedSubmission.getTaskId(),
                        savedSubmission.getHomeworkItemId(), savedSubmission.getStatus()));
        return SubmissionResult.from(savedSubmission).withCodeSubmission(savedCode, true);
    }
}
