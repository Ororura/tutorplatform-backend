package com.tutorplatform.submission.infrastructure.persistence;

import com.tutorplatform.submission.application.SubmissionQuery;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class JpaSubmissionQuery implements SubmissionQuery {

    private final SubmissionDatabaseRepository databaseRepository;

    JpaSubmissionQuery(SubmissionDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public Set<PassedHomeworkItem> findPassedHomeworkItems(
        UUID studentId,
        UUID studentProgramId,
        Set<UUID> homeworkItemIds
    ) {
        if (homeworkItemIds.isEmpty()) {
            return Set.of();
        }
        return databaseRepository.findPassedHomeworkItems(
                studentId, studentProgramId, homeworkItemIds
            ).stream()
            .map(row -> new PassedHomeworkItem((UUID) row[0], (UUID) row[1]))
            .collect(Collectors.toUnmodifiableSet());
    }
}
