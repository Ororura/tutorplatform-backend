package com.tutorplatform.session.infrastructure.persistence;

import com.tutorplatform.session.application.AttendedLessonSession;
import com.tutorplatform.session.application.LearningSessionQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JpaLearningSessionQuery implements LearningSessionQuery {

    private final LessonSessionDatabaseRepository databaseRepository;

    JpaLearningSessionQuery(LessonSessionDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public List<AttendedLessonSession> findAttendedByStudentProgram(UUID studentProgramId) {
        return databaseRepository.findAttendedByStudentProgram(studentProgramId).stream()
            .map(model -> {
                var session = model.toEntity();
                return new AttendedLessonSession(
                    session.id(), session.startedAt(), session.durationMinutes()
                );
            })
            .toList();
    }
}
