package com.tutorplatform.homework.application;

import com.tutorplatform.homework.domain.HomeworkEntity;
import com.tutorplatform.homework.domain.HomeworkItemEntity;
import com.tutorplatform.homework.domain.HomeworkRepository;
import com.tutorplatform.homework.domain.HomeworkStatus;
import com.tutorplatform.submission.application.SubmissionQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class HomeworkCompletionService {

    private final HomeworkRepository homeworkRepository;
    private final SubmissionQuery submissionQuery;

    public HomeworkCompletionService(
        HomeworkRepository homeworkRepository,
        SubmissionQuery submissionQuery
    ) {
        this.homeworkRepository = homeworkRepository;
        this.submissionQuery = submissionQuery;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recalculate(
        UUID homeworkItemId,
        UUID studentId,
        UUID studentProgramId,
        UUID taskId
    ) {
        HomeworkEntity homework = homeworkRepository.findByHomeworkItemIdWithItems(homeworkItemId)
            .orElse(null);
        if (homework == null
            || homework.getStatus() != HomeworkStatus.ASSIGNED
            || !homework.getStudentProgramId().equals(studentProgramId)) {
            return;
        }
        HomeworkItemEntity triggeringItem = homework.getItems().stream()
            .filter(item -> item.id().equals(homeworkItemId) && item.taskId().equals(taskId))
            .findFirst()
            .orElse(null);
        if (triggeringItem == null) {
            return;
        }

        List<HomeworkItemEntity> requiredItems = homework.getItems().stream()
            .filter(HomeworkItemEntity::required)
            .toList();
        if (requiredItems.isEmpty()) {
            return;
        }
        Set<UUID> requiredItemIds = requiredItems.stream()
            .map(HomeworkItemEntity::id)
            .collect(Collectors.toUnmodifiableSet());
        Set<SubmissionQuery.PassedHomeworkItem> passedItems = submissionQuery.findPassedHomeworkItems(
            studentId, studentProgramId, requiredItemIds
        );
        boolean allRequiredItemsPassed = requiredItems.stream().allMatch(requiredItem ->
            passedItems.contains(new SubmissionQuery.PassedHomeworkItem(
                requiredItem.id(), requiredItem.taskId()
            ))
        );
        if (allRequiredItemsPassed) {
            homework.complete(Instant.now());
            homeworkRepository.saveAndFlush(homework);
        }
    }
}
