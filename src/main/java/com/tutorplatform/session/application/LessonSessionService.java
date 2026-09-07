package com.tutorplatform.session.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.application.ProgramQuery;
import com.tutorplatform.session.application.exception.InvalidLessonSessionTopicsException;
import com.tutorplatform.session.application.exception.InvalidSessionListParameterException;
import com.tutorplatform.session.application.exception.LessonSessionNotFoundException;
import com.tutorplatform.session.application.exception.StudentProgramNotFoundException;
import com.tutorplatform.session.application.exception.TopicOutsideStudentProgramException;
import com.tutorplatform.session.domain.LessonSessionEntity;
import com.tutorplatform.session.domain.LessonSessionRepository;
import com.tutorplatform.session.domain.LessonSessionTopicEntity;
import com.tutorplatform.session.domain.LessonSessionTopicRepository;
import com.tutorplatform.student.application.StudentOwnershipQuery;
import com.tutorplatform.student.application.exception.StudentNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class LessonSessionService {

    private final StudentOwnershipQuery studentOwnershipQuery;
    private final ProgramQuery programQuery;
    private final LessonSessionRepository lessonSessionRepository;
    private final LessonSessionQuery lessonSessionQuery;
    private final LessonSessionTopicRepository lessonSessionTopicRepository;

    public LessonSessionService(
        StudentOwnershipQuery studentOwnershipQuery,
        ProgramQuery programQuery,
        LessonSessionRepository lessonSessionRepository,
        LessonSessionQuery lessonSessionQuery,
        LessonSessionTopicRepository lessonSessionTopicRepository
    ) {
        this.studentOwnershipQuery = studentOwnershipQuery;
        this.programQuery = programQuery;
        this.lessonSessionRepository = lessonSessionRepository;
        this.lessonSessionQuery = lessonSessionQuery;
        this.lessonSessionTopicRepository = lessonSessionTopicRepository;
    }

    @Transactional
    public LessonSessionResult createLessonSession(
        AuthenticatedUser principal,
        CreateLessonSessionCommand command
    ) {
        UUID teacherId = currentTeacherId(principal);
        requireOwnedStudent(teacherId, command.studentId());
        ProgramQuery.StudentProgramContext studentProgram = requireStudentProgram(
            command.studentProgramId(), command.studentId(), teacherId
        );
        validateTopics(command.topics(), studentProgram.learningProgramId());

        LessonSessionEntity lessonSession = lessonSessionRepository.saveAndFlush(new LessonSessionEntity(
            UUID.randomUUID(),
            studentProgram.id(),
            teacherId,
            command.startedAt(),
            command.durationMinutes(),
            command.attendanceStatus(),
            command.summary(),
            command.privateNotes()
        ));
        List<LessonSessionTopicEntity> topics = saveTopics(lessonSession.getId(), command.topics());
        return toResult(lessonSession, topics);
    }

    @Transactional(readOnly = true)
    public LessonSessionResult getLessonSession(
        AuthenticatedUser principal,
        UUID studentId,
        UUID lessonSessionId
    ) {
        UUID teacherId = currentTeacherId(principal);
        requireOwnedStudent(teacherId, studentId);
        LessonSessionEntity lessonSession = lessonSessionRepository
            .findOwnedById(lessonSessionId, teacherId, studentId)
            .orElseThrow(LessonSessionNotFoundException::new);
        return toResult(
            lessonSession,
            lessonSessionTopicRepository.findAllByLessonSessionId(lessonSessionId)
        );
    }

    @Transactional(readOnly = true)
    public LessonSessionPageResult listLessonSessions(
        AuthenticatedUser principal,
        UUID studentId,
        int page,
        int size
    ) {
        validatePagination(page, size);
        UUID teacherId = currentTeacherId(principal);
        requireOwnedStudent(teacherId, studentId);
        LessonSessionPage sessions = lessonSessionQuery.findPageByTeacherAndStudent(
            teacherId, studentId, page, size
        );
        Set<UUID> sessionIds = sessions.items().stream()
            .map(LessonSessionEntity::getId)
            .collect(java.util.stream.Collectors.toSet());
        Map<UUID, List<LessonSessionTopicEntity>> topicsBySession = groupTopics(
            lessonSessionTopicRepository.findAllByLessonSessionIds(sessionIds)
        );
        return new LessonSessionPageResult(
            sessions.items().stream()
                .map(session -> toResult(
                    session,
                    topicsBySession.getOrDefault(session.getId(), List.of())
                ))
                .toList(),
            page,
            size,
            sessions.totalElements(),
            sessions.totalPages()
        );
    }

    @Transactional
    public LessonSessionResult updateLessonSession(
        AuthenticatedUser principal,
        UUID studentId,
        UUID lessonSessionId,
        UpdateLessonSessionCommand command
    ) {
        UUID teacherId = currentTeacherId(principal);
        requireOwnedStudent(teacherId, studentId);
        LessonSessionEntity current = lessonSessionRepository
            .findOwnedById(lessonSessionId, teacherId, studentId)
            .orElseThrow(LessonSessionNotFoundException::new);
        ProgramQuery.StudentProgramContext studentProgram = requireStudentProgram(
            current.getStudentProgramId(), studentId, teacherId
        );
        validateTopics(command.topics(), studentProgram.learningProgramId());

        LessonSessionEntity updated = lessonSessionRepository.saveAndFlush(new LessonSessionEntity(
            current.getId(),
            current.getStudentProgramId(),
            current.getTeacherId(),
            command.startedAt(),
            command.durationMinutes(),
            command.attendanceStatus(),
            command.summary(),
            command.privateNotes(),
            command.version(),
            current.getCreatedAt(),
            current.getUpdatedAt()
        ));
        lessonSessionTopicRepository.deleteAllByLessonSessionId(lessonSessionId);
        List<LessonSessionTopicEntity> topics = saveTopics(lessonSessionId, command.topics());
        return toResult(updated, topics);
    }

    private UUID currentTeacherId(AuthenticatedUser principal) {
        return studentOwnershipQuery.findTeacherIdByUserId(principal.id()).orElseThrow();
    }

    private void requireOwnedStudent(UUID teacherId, UUID studentId) {
        if (!studentOwnershipQuery.isActivePrimaryOwner(teacherId, studentId)) {
            throw new StudentNotFoundException();
        }
    }

    private ProgramQuery.StudentProgramContext requireStudentProgram(
        UUID studentProgramId,
        UUID studentId,
        UUID teacherId
    ) {
        ProgramQuery.StudentProgramContext studentProgram = programQuery
            .findStudentProgram(studentProgramId)
            .orElseThrow(StudentProgramNotFoundException::new);
        if (!studentProgram.belongsToStudent(studentId) || !studentProgram.isAssignedBy(teacherId)) {
            throw new StudentProgramNotFoundException();
        }
        return studentProgram;
    }

    private void validateTopics(List<LessonSessionTopicInput> topics, UUID learningProgramId) {
        Set<UUID> topicIds = new HashSet<>();
        int primaryCount = 0;
        for (LessonSessionTopicInput topic : topics) {
            if (!topicIds.add(topic.topicId())) {
                throw new InvalidLessonSessionTopicsException("duplicate topicId");
            }
            if (topic.primary() && ++primaryCount > 1) {
                throw new InvalidLessonSessionTopicsException("only one primary topic is allowed");
            }
        }
        for (UUID topicId : topicIds) {
            if (!programQuery.topicBelongsToLearningProgram(topicId, learningProgramId)) {
                throw new TopicOutsideStudentProgramException();
            }
        }
    }

    private List<LessonSessionTopicEntity> saveTopics(
        UUID lessonSessionId,
        List<LessonSessionTopicInput> topics
    ) {
        return lessonSessionTopicRepository.saveAllAndFlush(topics.stream()
            .map(topic -> new LessonSessionTopicEntity(
                lessonSessionId, topic.topicId(), topic.primary()
            ))
            .toList());
    }

    private Map<UUID, List<LessonSessionTopicEntity>> groupTopics(
        List<LessonSessionTopicEntity> topics
    ) {
        Map<UUID, List<LessonSessionTopicEntity>> result = new HashMap<>();
        for (LessonSessionTopicEntity topic : topics) {
            result.computeIfAbsent(topic.getLessonSessionId(), ignored -> new java.util.ArrayList<>())
                .add(topic);
        }
        return result;
    }

    private LessonSessionResult toResult(
        LessonSessionEntity lessonSession,
        List<LessonSessionTopicEntity> topics
    ) {
        return new LessonSessionResult(
            lessonSession.getId(),
            lessonSession.getStudentProgramId(),
            lessonSession.getTeacherId(),
            lessonSession.getStartedAt(),
            lessonSession.getDurationMinutes(),
            lessonSession.getAttendanceStatus(),
            lessonSession.getSummary(),
            lessonSession.getPrivateNotes(),
            lessonSession.getVersion(),
            lessonSession.getCreatedAt(),
            lessonSession.getUpdatedAt(),
            topics.stream()
                .map(topic -> new LessonSessionTopicResult(
                    topic.getTopicId(), topic.isPrimary(), topic.getCreatedAt()
                ))
                .toList()
        );
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new InvalidSessionListParameterException(
                "page", "must be greater than or equal to 0"
            );
        }
        if (size < 1 || size > 100) {
            throw new InvalidSessionListParameterException("size", "must be between 1 and 100");
        }
    }
}
