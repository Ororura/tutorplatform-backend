package com.tutorplatform.submission.infrastructure.persistence;

import com.tutorplatform.submission.application.SubmissionQuery;
import com.tutorplatform.submission.domain.SubmissionStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSubmissionQuery implements SubmissionQuery {

    private final SubmissionDatabaseRepository databaseRepository;

    JpaSubmissionQuery(SubmissionDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public Set<PassedHomeworkItem> findPassedHomeworkItems(
            UUID studentId, UUID studentProgramId, Set<UUID> homeworkItemIds) {
        if (homeworkItemIds.isEmpty()) {
            return Set.of();
        }
        return databaseRepository
                .findPassedHomeworkItems(studentId, studentProgramId, homeworkItemIds)
                .stream()
                .map(row -> new PassedHomeworkItem((UUID) row[0], (UUID) row[1]))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Map<UUID, HomeworkItemSubmissionState> findHomeworkItemStates(
            UUID studentId, UUID studentProgramId, Set<UUID> homeworkItemIds) {
        if (homeworkItemIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, MutableHomeworkItemState> states = new LinkedHashMap<>();
        for (Object[] row :
                databaseRepository.findHomeworkItemSubmissionStates(
                        studentId, studentProgramId, homeworkItemIds)) {
            UUID homeworkItemId = (UUID) row[0];
            SubmissionStatus status = (SubmissionStatus) row[1];
            MutableHomeworkItemState state =
                    states.computeIfAbsent(
                            homeworkItemId, ignored -> new MutableHomeworkItemState(status));
            if (status == SubmissionStatus.PASSED) {
                state.passed = true;
            }
        }
        return states.entrySet().stream()
                .collect(
                        Collectors.toUnmodifiableMap(
                                Map.Entry::getKey,
                                entry ->
                                        new HomeworkItemSubmissionState(
                                                entry.getValue().passed,
                                                entry.getValue().latestSubmissionStatus)));
    }

    private static final class MutableHomeworkItemState {
        private final SubmissionStatus latestSubmissionStatus;
        private boolean passed;

        private MutableHomeworkItemState(SubmissionStatus latestSubmissionStatus) {
            this.latestSubmissionStatus = latestSubmissionStatus;
        }
    }
}
