package com.tutorplatform.homework.infrastructure.persistence;

import com.tutorplatform.homework.domain.HomeworkStatus;
import com.tutorplatform.submission.application.SubmissionHomeworkContextQuery;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSubmissionHomeworkContextQuery implements SubmissionHomeworkContextQuery {

    private final EntityManager entityManager;

    JpaSubmissionHomeworkContextQuery(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<HomeworkSubmissionContext> findSubmissionContext(UUID homeworkItemId) {
        return entityManager
                .createQuery(
                        """
                select homework.id, homework.studentProgramId, homework.assignedByTeacherId,
                       homework.status, item.id, item.taskId
                from HomeworkDatabaseModel homework
                join homework.items item
                where item.id = :homeworkItemId
                """,
                        Object[].class)
                .setParameter("homeworkItemId", homeworkItemId)
                .getResultList()
                .stream()
                .findFirst()
                .map(
                        row ->
                                new HomeworkSubmissionContext(
                                        (UUID) row[0],
                                        (UUID) row[1],
                                        (UUID) row[2],
                                        row[3] == HomeworkStatus.CANCELLED,
                                        row[3] == HomeworkStatus.COMPLETED,
                                        (UUID) row[4],
                                        (UUID) row[5]));
    }
}
