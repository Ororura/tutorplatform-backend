package com.tutorplatform.session.infrastructure.persistence;

import com.tutorplatform.session.application.LessonSessionContext;
import com.tutorplatform.session.application.LessonSessionPage;
import com.tutorplatform.session.application.LessonSessionQuery;
import com.tutorplatform.session.domain.AttendanceStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
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
            boolean ascending,
            AttendanceStatus attendanceStatus,
            Instant startedBefore) {
        Sort.Direction direction = ascending ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(new Sort.Order(direction, sortField), Sort.Order.desc("id")));
        Page<LessonSessionDatabaseModel> result;
        if (attendanceStatus == null && startedBefore == null) {
            result = databaseRepository.findPageByTeacherAndStudent(teacherId, studentId, pageable);
        } else if (attendanceStatus == null) {
            result =
                    databaseRepository.findPageByTeacherAndStudentAndStartedAtLessThanEqual(
                            teacherId, studentId, startedBefore, pageable);
        } else if (startedBefore == null) {
            result =
                    databaseRepository.findPageByTeacherAndStudentAndAttendanceStatus(
                            teacherId, studentId, attendanceStatus, pageable);
        } else {
            result =
                    databaseRepository
                            .findPageByTeacherAndStudentAndAttendanceStatusAndStartedAtLessThanEqual(
                                    teacherId,
                                    studentId,
                                    attendanceStatus,
                                    startedBefore,
                                    pageable);
        }
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
