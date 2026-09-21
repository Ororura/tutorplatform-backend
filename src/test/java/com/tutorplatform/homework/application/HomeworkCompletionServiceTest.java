package com.tutorplatform.homework.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.tutorplatform.homework.domain.HomeworkEntity;
import com.tutorplatform.homework.domain.HomeworkItemEntity;
import com.tutorplatform.homework.domain.HomeworkRepository;
import com.tutorplatform.homework.domain.HomeworkStatus;
import com.tutorplatform.submission.application.SubmissionQuery;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HomeworkCompletionServiceTest {

    private static final UUID HOMEWORK_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID FIRST_ITEM_ID = UUID.randomUUID();
    private static final UUID SECOND_ITEM_ID = UUID.randomUUID();
    private static final UUID FIRST_TASK_ID = UUID.randomUUID();
    private static final UUID SECOND_TASK_ID = UUID.randomUUID();

    @Mock private HomeworkRepository homeworkRepository;
    @Mock private SubmissionQuery submissionQuery;
    private HomeworkCompletionService service;

    @BeforeEach
    void setUp() {
        service = new HomeworkCompletionService(homeworkRepository, submissionQuery);
    }

    @Test
    void oneRequiredItemWithPassedSubmissionCompletesHomework() {
        HomeworkEntity homework = assigned(item(FIRST_ITEM_ID, FIRST_TASK_ID, true));
        arrange(homework, passed(FIRST_ITEM_ID, FIRST_TASK_ID));

        recalculate();

        assertCompleted(homework);
    }

    @Test
    void twoRequiredItemsWithPassedSubmissionsCompleteHomework() {
        HomeworkEntity homework =
                assigned(
                        item(FIRST_ITEM_ID, FIRST_TASK_ID, true),
                        item(SECOND_ITEM_ID, SECOND_TASK_ID, true));
        arrange(
                homework,
                passed(FIRST_ITEM_ID, FIRST_TASK_ID),
                passed(SECOND_ITEM_ID, SECOND_TASK_ID));

        recalculate();

        assertCompleted(homework);
    }

    @Test
    void needsReviewRequiredItemLeavesHomeworkAssigned() {
        assertIncompleteWhenOnlyFirstItemHasPassedFact();
    }

    @Test
    void failedRequiredItemLeavesHomeworkAssigned() {
        assertIncompleteWhenOnlyFirstItemHasPassedFact();
    }

    @Test
    void failedAttemptFollowedByPassedAttemptCountsAsComplete() {
        HomeworkEntity homework = assigned(item(FIRST_ITEM_ID, FIRST_TASK_ID, true));
        arrange(homework, passed(FIRST_ITEM_ID, FIRST_TASK_ID));

        recalculate();

        assertCompleted(homework);
    }

    @Test
    void multiplePassedAttemptsDoNotBreakCompletion() {
        HomeworkEntity homework = assigned(item(FIRST_ITEM_ID, FIRST_TASK_ID, true));
        arrange(homework, passed(FIRST_ITEM_ID, FIRST_TASK_ID));

        recalculate();

        assertCompleted(homework);
    }

    @Test
    void optionalItemWithoutSubmissionDoesNotBlockCompletion() {
        HomeworkEntity homework =
                assigned(
                        item(FIRST_ITEM_ID, FIRST_TASK_ID, true),
                        item(SECOND_ITEM_ID, SECOND_TASK_ID, false));
        arrange(homework, passed(FIRST_ITEM_ID, FIRST_TASK_ID));

        recalculate();

        assertCompleted(homework);
    }

    @Test
    void homeworkWithOnlyOptionalItemsStaysAssigned() {
        HomeworkEntity homework = assigned(item(FIRST_ITEM_ID, FIRST_TASK_ID, false));
        when(homeworkRepository.findByHomeworkItemIdWithItems(FIRST_ITEM_ID))
                .thenReturn(Optional.of(homework));

        recalculate();

        assertThat(homework.getStatus()).isEqualTo(HomeworkStatus.ASSIGNED);
        verifyNoInteractions(submissionQuery);
        verify(homeworkRepository, never()).saveAndFlush(any());
    }

    @Test
    void passedSubmissionForAnotherHomeworkItemIsIgnored() {
        HomeworkEntity homework = assigned(item(FIRST_ITEM_ID, FIRST_TASK_ID, true));
        arrange(homework, passed(UUID.randomUUID(), FIRST_TASK_ID));

        recalculate();

        assertAssigned(homework);
    }

    @Test
    void eventForAnotherStudentProgramIsIgnored() {
        HomeworkEntity homework = assigned(item(FIRST_ITEM_ID, FIRST_TASK_ID, true));
        when(homeworkRepository.findByHomeworkItemIdWithItems(FIRST_ITEM_ID))
                .thenReturn(Optional.of(homework));

        service.recalculate(FIRST_ITEM_ID, STUDENT_ID, UUID.randomUUID(), FIRST_TASK_ID);

        assertAssigned(homework);
        verifyNoInteractions(submissionQuery);
    }

    @Test
    void passedSubmissionForAnotherTaskIsIgnored() {
        HomeworkEntity homework = assigned(item(FIRST_ITEM_ID, FIRST_TASK_ID, true));
        arrange(homework, passed(FIRST_ITEM_ID, UUID.randomUUID()));

        recalculate();

        assertAssigned(homework);
    }

    @Test
    void practiceSubmissionWithoutHomeworkItemIsIgnored() {
        HomeworkEntity homework = assigned(item(FIRST_ITEM_ID, FIRST_TASK_ID, true));
        arrange(homework);

        recalculate();

        assertAssigned(homework);
    }

    @Test
    void cancelledHomeworkNeverCompletes() {
        HomeworkEntity homework =
                homework(HomeworkStatus.CANCELLED, item(FIRST_ITEM_ID, FIRST_TASK_ID, true));
        when(homeworkRepository.findByHomeworkItemIdWithItems(FIRST_ITEM_ID))
                .thenReturn(Optional.of(homework));

        recalculate();

        assertThat(homework.getStatus()).isEqualTo(HomeworkStatus.CANCELLED);
        verifyNoInteractions(submissionQuery);
    }

    @Test
    void alreadyCompletedHomeworkIsNotRecalculated() {
        Instant completedAt = Instant.parse("2026-09-01T10:00:00Z");
        HomeworkEntity homework =
                new HomeworkEntity(
                        HOMEWORK_ID,
                        PROGRAM_ID,
                        TEACHER_ID,
                        "Homework",
                        null,
                        Instant.now(),
                        null,
                        HomeworkStatus.COMPLETED,
                        completedAt,
                        List.of(item(FIRST_ITEM_ID, FIRST_TASK_ID, true)));
        when(homeworkRepository.findByHomeworkItemIdWithItems(FIRST_ITEM_ID))
                .thenReturn(Optional.of(homework));

        recalculate();

        assertThat(homework.getCompletedAt()).isEqualTo(completedAt);
        verifyNoInteractions(submissionQuery);
        verify(homeworkRepository, never()).saveAndFlush(any());
    }

    @Test
    void completionSetsCompletedAt() {
        HomeworkEntity homework = assigned(item(FIRST_ITEM_ID, FIRST_TASK_ID, true));
        arrange(homework, passed(FIRST_ITEM_ID, FIRST_TASK_ID));
        Instant before = Instant.now();

        recalculate();

        assertThat(homework.getCompletedAt()).isBetween(before, Instant.now());
    }

    @Test
    void repeatedRecalculationDoesNotChangeCompletedAt() {
        HomeworkEntity homework = assigned(item(FIRST_ITEM_ID, FIRST_TASK_ID, true));
        arrange(homework, passed(FIRST_ITEM_ID, FIRST_TASK_ID));

        recalculate();
        Instant completedAt = homework.getCompletedAt();
        recalculate();

        assertThat(homework.getCompletedAt()).isEqualTo(completedAt);
        verify(homeworkRepository, times(1)).saveAndFlush(homework);
        verify(submissionQuery, times(1)).findPassedHomeworkItems(any(), any(), any());
    }

    @Test
    void completionUsesOneBatchSubmissionQueryForManyItems() {
        HomeworkEntity homework =
                assigned(
                        item(FIRST_ITEM_ID, FIRST_TASK_ID, true),
                        item(SECOND_ITEM_ID, SECOND_TASK_ID, true));
        arrange(
                homework,
                passed(FIRST_ITEM_ID, FIRST_TASK_ID),
                passed(SECOND_ITEM_ID, SECOND_TASK_ID));

        recalculate();

        verify(submissionQuery, times(1))
                .findPassedHomeworkItems(
                        STUDENT_ID, PROGRAM_ID, Set.of(FIRST_ITEM_ID, SECOND_ITEM_ID));
    }

    private void assertIncompleteWhenOnlyFirstItemHasPassedFact() {
        HomeworkEntity homework =
                assigned(
                        item(FIRST_ITEM_ID, FIRST_TASK_ID, true),
                        item(SECOND_ITEM_ID, SECOND_TASK_ID, true));
        arrange(homework, passed(FIRST_ITEM_ID, FIRST_TASK_ID));

        recalculate();

        assertAssigned(homework);
    }

    private void arrange(
            HomeworkEntity homework, SubmissionQuery.PassedHomeworkItem... passedItems) {
        when(homeworkRepository.findByHomeworkItemIdWithItems(FIRST_ITEM_ID))
                .thenReturn(Optional.of(homework));
        Set<UUID> requiredIds =
                homework.getItems().stream()
                        .filter(HomeworkItemEntity::required)
                        .map(HomeworkItemEntity::id)
                        .collect(java.util.stream.Collectors.toSet());
        when(submissionQuery.findPassedHomeworkItems(STUDENT_ID, PROGRAM_ID, requiredIds))
                .thenReturn(Set.of(passedItems));
    }

    private void recalculate() {
        service.recalculate(FIRST_ITEM_ID, STUDENT_ID, PROGRAM_ID, FIRST_TASK_ID);
    }

    private void assertCompleted(HomeworkEntity homework) {
        assertThat(homework.getStatus()).isEqualTo(HomeworkStatus.COMPLETED);
        assertThat(homework.getCompletedAt()).isNotNull();
        verify(homeworkRepository).saveAndFlush(homework);
    }

    private void assertAssigned(HomeworkEntity homework) {
        assertThat(homework.getStatus()).isEqualTo(HomeworkStatus.ASSIGNED);
        assertThat(homework.getCompletedAt()).isNull();
        verify(homeworkRepository, never()).saveAndFlush(any());
    }

    private HomeworkEntity assigned(HomeworkItemEntity... items) {
        return homework(HomeworkStatus.ASSIGNED, items);
    }

    private HomeworkEntity homework(HomeworkStatus status, HomeworkItemEntity... items) {
        return new HomeworkEntity(
                HOMEWORK_ID,
                PROGRAM_ID,
                TEACHER_ID,
                "Homework",
                null,
                Instant.now(),
                null,
                status,
                null,
                List.of(items));
    }

    private HomeworkItemEntity item(UUID itemId, UUID taskId, boolean required) {
        return new HomeworkItemEntity(itemId, HOMEWORK_ID, taskId, 0, required);
    }

    private SubmissionQuery.PassedHomeworkItem passed(UUID itemId, UUID taskId) {
        return new SubmissionQuery.PassedHomeworkItem(itemId, taskId);
    }
}
