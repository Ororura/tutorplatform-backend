package com.tutorplatform.submission.infrastructure.persistence;

import com.tutorplatform.submission.domain.SubmissionAttemptContext;
import com.tutorplatform.submission.domain.SubmissionEntity;
import com.tutorplatform.submission.domain.SubmissionPage;
import com.tutorplatform.submission.domain.SubmissionRepository;
import com.tutorplatform.submission.domain.SubmissionStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaSubmissionRepository implements SubmissionRepository {

    private static final Sort NEWEST_FIRST = Sort.by(
            Sort.Order.desc("submittedAt"),
            Sort.Order.desc("id")
    );
    private static final Sort ATTEMPTS_NEWEST_FIRST = Sort.by(
            Sort.Order.desc("attemptNo"),
            Sort.Order.desc("id")
    );

    private final SubmissionDatabaseRepository databaseRepository;

    JpaSubmissionRepository(SubmissionDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public SubmissionEntity saveAndFlush(SubmissionEntity submission) {
        return databaseRepository.saveAndFlush(new SubmissionDatabaseModel(submission)).toEntity();
    }

    @Override
    public Optional<SubmissionEntity> findById(UUID submissionId) {
        return databaseRepository.findById(submissionId).map(SubmissionDatabaseModel::toEntity);
    }

    @Override
    public Optional<SubmissionEntity> findByIdAndStudentId(UUID submissionId, UUID studentId) {
        return databaseRepository.findByIdAndStudentId(submissionId, studentId)
                .map(SubmissionDatabaseModel::toEntity);
    }

    @Override
    public SubmissionPage findPageByStudentId(UUID studentId, int page, int size) {
        return toPage(databaseRepository.findAllByStudentId(
                studentId, PageRequest.of(page, size, NEWEST_FIRST)
        ));
    }

    @Override
    public SubmissionPage findAttempts(SubmissionAttemptContext context, int page, int size) {
        return toPage(databaseRepository.findAttempts(
                context.studentId(), context.studentProgramId(), context.taskId(), context.homeworkItemId(),
                PageRequest.of(page, size, ATTEMPTS_NEWEST_FIRST)
        ));
    }

    @Override
    public Optional<SubmissionEntity> findLatestAttempt(SubmissionAttemptContext context) {
        return databaseRepository.findLatestAttempt(
                context.studentId(), context.studentProgramId(), context.taskId(), context.homeworkItemId()
        ).map(SubmissionDatabaseModel::toEntity);
    }

    @Override
    public boolean existsByStatus(SubmissionAttemptContext context, SubmissionStatus status) {
        return databaseRepository.existsByStatus(
                context.studentId(), context.studentProgramId(), context.taskId(), context.homeworkItemId(), status
        );
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int nextAttemptNo(SubmissionAttemptContext context) {
        databaseRepository.lockStudentProgram(context.studentProgramId());
        return databaseRepository.findMaxAttemptNo(
                context.studentId(), context.studentProgramId(), context.taskId(), context.homeworkItemId()
        ) + 1;
    }

    private SubmissionPage toPage(org.springframework.data.domain.Page<SubmissionDatabaseModel> page) {
        return new SubmissionPage(
                page.getContent().stream().map(SubmissionDatabaseModel::toEntity).toList(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
