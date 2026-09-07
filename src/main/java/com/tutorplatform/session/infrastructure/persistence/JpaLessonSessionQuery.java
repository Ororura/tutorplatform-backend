package com.tutorplatform.session.infrastructure.persistence;

import com.tutorplatform.session.application.LessonSessionPage;
import com.tutorplatform.session.application.LessonSessionQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class JpaLessonSessionQuery implements LessonSessionQuery {

    private final LessonSessionDatabaseRepository databaseRepository;

    JpaLessonSessionQuery(LessonSessionDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public LessonSessionPage findPageByTeacherAndStudent(
        UUID teacherId,
        UUID studentId,
        int page,
        int size
    ) {
        var result = databaseRepository.findPageByTeacherAndStudent(
            teacherId,
            studentId,
            PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("startedAt"),
                Sort.Order.desc("id")
            ))
        );
        return new LessonSessionPage(
            result.getContent().stream().map(LessonSessionDatabaseModel::toEntity).toList(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }
}
