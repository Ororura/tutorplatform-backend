package com.tutorplatform.submission.infrastructure.persistence;

import com.tutorplatform.submission.domain.CodeExecutionStatus;
import com.tutorplatform.submission.domain.CodeSubmissionEntity;
import com.tutorplatform.submission.domain.CodeSubmissionRepository;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCodeSubmissionRepository implements CodeSubmissionRepository {

    private final CodeSubmissionDatabaseRepository databaseRepository;

    JpaCodeSubmissionRepository(CodeSubmissionDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public CodeSubmissionEntity saveAndFlush(CodeSubmissionEntity codeSubmission) {
        return databaseRepository
                .saveAndFlush(new CodeSubmissionDatabaseModel(codeSubmission))
                .toEntity();
    }

    @Override
    public Optional<CodeSubmissionEntity> findBySubmissionId(UUID submissionId) {
        return databaseRepository.findById(submissionId).map(CodeSubmissionDatabaseModel::toEntity);
    }

    @Override
    public Map<UUID, Summary> findSummaries(Collection<UUID> submissionIds) {
        if (submissionIds.isEmpty()) {
            return Map.of();
        }
        return databaseRepository.findSummaries(submissionIds).stream()
                .map(
                        row ->
                                new Summary(
                                        (UUID) row[0],
                                        (CodeExecutionStatus) row[1],
                                        (Integer) row[2],
                                        (Integer) row[3],
                                        (Integer) row[4],
                                        (String) row[5],
                                        (String) row[6]))
                .collect(Collectors.toUnmodifiableMap(Summary::submissionId, Function.identity()));
    }
}
