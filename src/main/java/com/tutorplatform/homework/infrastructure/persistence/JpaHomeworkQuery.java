package com.tutorplatform.homework.infrastructure.persistence;

import com.tutorplatform.homework.application.HomeworkListItem;
import com.tutorplatform.homework.application.HomeworkPage;
import com.tutorplatform.homework.application.HomeworkQuery;
import com.tutorplatform.homework.application.StudentHomeworkDetails;
import com.tutorplatform.homework.application.StudentHomeworkListItem;
import com.tutorplatform.homework.application.StudentHomeworkPage;
import com.tutorplatform.homework.domain.HomeworkStatus;
import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskType;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JpaHomeworkQuery implements HomeworkQuery {

    private final HomeworkDatabaseRepository databaseRepository;
    private final EntityManager entityManager;

    JpaHomeworkQuery(HomeworkDatabaseRepository databaseRepository, EntityManager entityManager) {
        this.databaseRepository = databaseRepository;
        this.entityManager = entityManager;
    }

    @Override
    public HomeworkPage findPageByTeacherAndStudent(
            UUID teacherId,
            UUID studentId,
            UUID studentProgramId,
            HomeworkStatus status,
            int page,
            int size,
            String sortField,
            boolean ascending
    ) {
        Sort.Direction direction = ascending ? Sort.Direction.ASC : Sort.Direction.DESC;
        var result = databaseRepository.findPageByTeacherAndStudent(
                teacherId,
                studentId,
                studentProgramId,
                status,
                PageRequest.of(page, size, Sort.by(
                        new Sort.Order(direction, sortField),
                        Sort.Order.desc("id")
                ))
        );
        return new HomeworkPage(
                result.getContent().stream()
                        .map(homework -> new HomeworkListItem(
                                homework.getId(),
                                homework.getStudentProgramId(),
                                homework.getTitle(),
                                homework.getStatus(),
                                homework.getAssignedAt(),
                                homework.getDueAt(),
                                homework.getCompletedAt(),
                                homework.getCreatedAt()
                        ))
                        .toList(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public StudentHomeworkPage findPageByStudent(
            UUID studentId,
            UUID studentProgramId,
            HomeworkStatus status,
            int page,
            int size,
            String sortField,
            boolean ascending
    ) {
        StringBuilder filters = new StringBuilder(
                " where homework.studentProgram.studentId = :studentId"
        );
        if (studentProgramId != null) {
            filters.append(" and homework.studentProgramId = :studentProgramId");
        }
        if (status != null) {
            filters.append(" and homework.status = :status");
        }
        String orderProperty = switch (sortField) {
            case "assignedAt" -> "homework.assignedAt";
            case "dueAt" -> "homework.dueAt";
            case "createdAt" -> "homework.createdAt";
            case "title" -> "homework.title";
            default -> throw new IllegalArgumentException("Unsupported homework sort field");
        };
        String direction = ascending ? "asc" : "desc";
        String pageHql = "select homework.id, homework.studentProgramId, homework.title, homework.status, "
                + "homework.assignedAt, homework.dueAt, homework.completedAt, "
                + "count(item.id), homework.createdAt "
                + "from HomeworkDatabaseModel homework left join homework.items item"
                + filters
                + " group by homework.id, homework.studentProgramId, homework.title, homework.status, "
                + "homework.assignedAt, homework.dueAt, homework.completedAt, homework.createdAt"
                + " order by " + orderProperty + " " + direction + ", homework.id desc";
        var query = entityManager.createQuery(pageHql, Object[].class);
        bindStudentFilters(query, studentId, studentProgramId, status);
        query.setFirstResult(page * size);
        query.setMaxResults(size);

        var countQuery = entityManager.createQuery(
                "select count(homework.id) from HomeworkDatabaseModel homework" + filters,
                Long.class
        );
        bindStudentFilters(countQuery, studentId, studentProgramId, status);
        long totalElements = countQuery.getSingleResult();
        List<StudentHomeworkListItem> items = query.getResultList().stream()
                .map(row -> new StudentHomeworkListItem(
                        (UUID) row[0],
                        (UUID) row[1],
                        (String) row[2],
                        (HomeworkStatus) row[3],
                        (Instant) row[4],
                        (Instant) row[5],
                        (Instant) row[6],
                        (Long) row[7],
                        (Instant) row[8]
                ))
                .toList();
        int totalPages = totalElements == 0 ? 0 : (int) ((totalElements + size - 1) / size);
        return new StudentHomeworkPage(items, totalElements, totalPages);
    }

    @Override
    public Optional<StudentHomeworkDetails> findDetailsByStudent(UUID studentId, UUID homeworkId) {
        List<Object[]> rows = entityManager.createQuery("""
                select homework.id, homework.studentProgramId, homework.title, homework.description,
                       homework.status, homework.assignedAt, homework.dueAt, homework.completedAt,
                       item.id, item.taskId, item.position, item.required,
                       task.id, task.title, task.descriptionMarkdown, task.taskType, task.difficulty
                from HomeworkDatabaseModel homework
                join homework.items item
                join item.task task
                where homework.id = :homeworkId
                  and homework.studentProgram.studentId = :studentId
                order by item.position asc
                """, Object[].class)
                .setParameter("homeworkId", homeworkId)
                .setParameter("studentId", studentId)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] first = rows.getFirst();
        List<StudentHomeworkDetails.Item> items = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            items.add(new StudentHomeworkDetails.Item(
                    (UUID) row[8],
                    (UUID) row[9],
                    (Integer) row[10],
                    (Boolean) row[11],
                    new StudentHomeworkDetails.Task(
                            (UUID) row[12],
                            (String) row[13],
                            (String) row[14],
                            (TaskType) row[15],
                            (TaskDifficulty) row[16]
                    )
            ));
        }
        return Optional.of(new StudentHomeworkDetails(
                (UUID) first[0],
                (UUID) first[1],
                (String) first[2],
                (String) first[3],
                (HomeworkStatus) first[4],
                (Instant) first[5],
                (Instant) first[6],
                (Instant) first[7],
                List.copyOf(items)
        ));
    }

    @Override
    public Optional<HomeworkSubmissionContext> findSubmissionContext(UUID homeworkItemId) {
        return entityManager.createQuery("""
                select homework.id, homework.studentProgramId, homework.assignedByTeacherId,
                       homework.status, item.id, item.taskId
                from HomeworkDatabaseModel homework
                join homework.items item
                where item.id = :homeworkItemId
                """, Object[].class)
                .setParameter("homeworkItemId", homeworkItemId)
                .getResultStream()
                .findFirst()
                .map(row -> new HomeworkSubmissionContext(
                        (UUID) row[0], (UUID) row[1], (UUID) row[2], (HomeworkStatus) row[3],
                        (UUID) row[4], (UUID) row[5]
                ));
    }

    private void bindStudentFilters(
            jakarta.persistence.Query query,
            UUID studentId,
            UUID studentProgramId,
            HomeworkStatus status
    ) {
        query.setParameter("studentId", studentId);
        if (studentProgramId != null) {
            query.setParameter("studentProgramId", studentProgramId);
        }
        if (status != null) {
            query.setParameter("status", status);
        }
    }
}
