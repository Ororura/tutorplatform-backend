package com.tutorplatform.session.infrastructure.persistence;

import com.tutorplatform.session.application.LessonSessionContext;
import com.tutorplatform.session.application.LessonSessionPage;
import com.tutorplatform.session.application.LessonSessionQuery;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaLessonSessionQuery implements LessonSessionQuery {

    private final LessonSessionDatabaseRepository databaseRepository;

    JpaLessonSessionQuery(LessonSessionDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public Optional<LessonSessionContext> findContextById(UUID lessonSessionId) {
        return databaseRepository.findById(lessonSessionId).flatMap(this::toContext);
    }

    @Override
    public Optional<LessonSessionContext> findContextByIdForUpdate(UUID lessonSessionId) {
        return databaseRepository.findWithLockById(lessonSessionId).flatMap(this::toContext);
    }

    @Override
    public LessonSessionPage findPageByTeacherAndStudent(
            UUID teacherId,
            UUID studentId,
            int page,
            int size,
            String sortField,
            boolean ascending) {
        Sort.Direction direction = ascending ? Sort.Direction.ASC : Sort.Direction.DESC;
        var result =
                databaseRepository.findPageByTeacherAndStudent(
                        teacherId,
                        studentId,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(
                                        new Sort.Order(direction, sortField),
                                        Sort.Order.desc("id"))));
        return new LessonSessionPage(
                result.getContent().stream().map(LessonSessionDatabaseModel::toEntity).toList(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private Optional<LessonSessionContext> toContext(LessonSessionDatabaseModel session) {
        var lessonSession = session.toEntity();
        return databaseRepository
                .findStudentIdById(lessonSession.id())
                .map(
                        studentId ->
                                new LessonSessionContext(
                                        lessonSession.id(),
                                        lessonSession.teacherId(),
                                        lessonSession.studentProgramId(),
                                        studentId));
    }
}
