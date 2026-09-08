package com.tutorplatform.homework.infrastructure.persistence;

import com.tutorplatform.homework.application.HomeworkListItem;
import com.tutorplatform.homework.application.HomeworkPage;
import com.tutorplatform.homework.application.HomeworkQuery;
import com.tutorplatform.homework.domain.HomeworkStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class JpaHomeworkQuery implements HomeworkQuery {

    private final HomeworkDatabaseRepository databaseRepository;

    JpaHomeworkQuery(HomeworkDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
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
}
