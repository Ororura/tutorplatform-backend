package com.tutorplatform.session.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.application.ProgramQuery;
import com.tutorplatform.session.application.exception.*;
import com.tutorplatform.session.domain.LessonSessionEntity;
import com.tutorplatform.session.domain.LessonSessionRepository;
import com.tutorplatform.session.domain.LessonSessionTopicEntity;
import com.tutorplatform.session.domain.LessonSessionTopicRepository;
import com.tutorplatform.shared.time.InstantPrecision;
import com.tutorplatform.student.application.management.StudentNotFoundException;
import com.tutorplatform.student.application.ownership.StudentOwnershipQuery;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LessonSessionService {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("startedAt", "createdAt", "durationMinutes");

    private final StudentOwnershipQuery studentOwnershipQuery;
    private final ProgramQuery programQuery;
    private final LessonSessionRepository lessonSessionRepository;
    private final LessonSessionQuery lessonSessionQuery;
    private final LessonSessionTopicRepository lessonSessionTopicRepository;
    private final ApplicationEventPublisher eventPublisher;

    public LessonSessionService(
            StudentOwnershipQuery studentOwnershipQuery,
            ProgramQuery programQuery,
            LessonSessionRepository lessonSessionRepository,
            LessonSessionQuery lessonSessionQuery,
            LessonSessionTopicRepository lessonSessionTopicRepository,
            ApplicationEventPublisher eventPublisher) {
        this.studentOwnershipQuery = studentOwnershipQuery;
        this.programQuery = programQuery;
        this.lessonSessionRepository = lessonSessionRepository;
        this.lessonSessionQuery = lessonSessionQuery;
        this.lessonSessionTopicRepository = lessonSessionTopicRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public LessonSessionResult createLessonSession(
            AuthenticatedUser principal, CreateLessonSessionCommand command) {
        UUID teacherId = currentTeacherId(principal);
        requireOwnedStudent(teacherId, command.studentId());
        ProgramQuery.StudentProgramContext studentProgram =
                requireStudentProgram(command.studentProgramId(), command.studentId(), teacherId);
        validateTopics(command.topics(), studentProgram.learningProgramId());

        LessonSessionEntity lessonSession =
                lessonSessionRepository.saveAndFlush(
                        new LessonSessionEntity(
                                UUID.randomUUID(),
                                studentProgram.id(),
                                teacherId,
                                InstantPrecision.database(command.startedAt()),
                                command.durationMinutes(),
                                command.attendanceStatus(),
                                command.summary(),
                                command.privateNotes()));
        List<LessonSessionTopicEntity> topics = saveTopics(lessonSession.id(), command.topics());
        publishChanged(lessonSession, null);
        return toResult(lessonSession, topics);
    }

    @Transactional(readOnly = true)
    public LessonSessionResult getLessonSession(
            AuthenticatedUser principal, UUID studentId, UUID lessonSessionId) {
        UUID teacherId = currentTeacherId(principal);
        requireOwnedStudent(teacherId, studentId);
        LessonSessionEntity lessonSession =
                lessonSessionRepository
                        .findOwnedById(lessonSessionId, teacherId, studentId)
                        .orElseThrow(LessonSessionNotFoundException::new);
        return toResult(
                lessonSession,
                lessonSessionTopicRepository.findAllByLessonSessionId(lessonSessionId));
    }

    @Transactional(readOnly = true)
    public LessonSessionPageResult listLessonSessions(
            AuthenticatedUser principal, UUID studentId, int page, int size) {
        return listLessonSessions(principal, studentId, page, size, "startedAt,desc");
    }

    @Transactional(readOnly = true)
    public LessonSessionPageResult listLessonSessions(
            AuthenticatedUser principal, UUID studentId, int page, int size, String sort) {
        SortParameters sortParameters = validateListParameters(page, size, sort);
        UUID teacherId = currentTeacherId(principal);
        requireOwnedStudent(teacherId, studentId);
        LessonSessionPage sessions =
                lessonSessionQuery.findPageByTeacherAndStudent(
                        teacherId,
                        studentId,
                        page,
                        size,
                        sortParameters.field(),
                        sortParameters.ascending());
        Set<UUID> sessionIds =
                sessions.items().stream()
                        .map(LessonSessionEntity::id)
                        .collect(java.util.stream.Collectors.toSet());
        Map<UUID, List<LessonSessionTopicEntity>> topicsBySession =
                groupTopics(lessonSessionTopicRepository.findAllByLessonSessionIds(sessionIds));
        return new LessonSessionPageResult(
                sessions.items().stream()
                        .map(
                                session ->
                                        toResult(
                                                session,
                                                topicsBySession.getOrDefault(
                                                        session.id(), List.of())))
                        .toList(),
                page,
                size,
                sessions.totalElements(),
                sessions.totalPages());
    }

    @Transactional
    public LessonSessionResult updateLessonSession(
            AuthenticatedUser principal,
            UUID studentId,
            UUID lessonSessionId,
            UpdateLessonSessionCommand command) {
        UUID teacherId = currentTeacherId(principal);
        requireOwnedStudent(teacherId, studentId);
        LessonSessionEntity current =
                lessonSessionRepository
                        .findOwnedById(lessonSessionId, teacherId, studentId)
                        .orElseThrow(LessonSessionNotFoundException::new);
        ProgramQuery.StudentProgramContext studentProgram =
                requireStudentProgram(current.studentProgramId(), studentId, teacherId);
        validateTopics(command.topics(), studentProgram.learningProgramId());

        LessonSessionEntity updated;
        try {
            updated =
                    lessonSessionRepository.saveAndFlush(
                            new LessonSessionEntity(
                                    current.id(),
                                    current.studentProgramId(),
                                    current.teacherId(),
                                    command.startedAt(),
                                    command.durationMinutes(),
                                    command.attendanceStatus(),
                                    command.summary(),
                                    command.privateNotes(),
                                    command.version(),
                                    current.createdAt(),
                                    current.updatedAt()));
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new LessonSessionVersionConflictException(exception);
        }
        lessonSessionTopicRepository.deleteAllByLessonSessionId(lessonSessionId);
        List<LessonSessionTopicEntity> topics = saveTopics(lessonSessionId, command.topics());
        publishChanged(updated, current);
        return toResult(updated, topics);
    }

    private void publishChanged(LessonSessionEntity session, LessonSessionEntity previous) {
        eventPublisher.publishEvent(
                new LessonSessionChangedEvent(
                        session.id(),
                        session.studentProgramId(),
                        session.attendanceStatus(),
                        session.startedAt(),
                        session.durationMinutes(),
                        previous == null ? null : previous.attendanceStatus(),
                        previous == null ? null : previous.startedAt(),
                        previous == null ? null : previous.durationMinutes()));
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
            UUID studentProgramId, UUID studentId, UUID teacherId) {
        ProgramQuery.StudentProgramContext studentProgram =
                programQuery
                        .findStudentProgram(studentProgramId)
                        .orElseThrow(StudentProgramNotFoundException::new);
        if (!studentProgram.belongsToStudent(studentId)
                || !studentProgram.isAssignedBy(teacherId)) {
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
            UUID lessonSessionId, List<LessonSessionTopicInput> topics) {
        return lessonSessionTopicRepository.saveAllAndFlush(
                topics.stream()
                        .map(
                                topic ->
                                        new LessonSessionTopicEntity(
                                                lessonSessionId, topic.topicId(), topic.primary()))
                        .toList());
    }

    private Map<UUID, List<LessonSessionTopicEntity>> groupTopics(
            List<LessonSessionTopicEntity> topics) {
        Map<UUID, List<LessonSessionTopicEntity>> result = new HashMap<>();
        for (LessonSessionTopicEntity topic : topics) {
            result.computeIfAbsent(topic.lessonSessionId(), ignored -> new java.util.ArrayList<>())
                    .add(topic);
        }
        return result;
    }

    private LessonSessionResult toResult(
            LessonSessionEntity lessonSession, List<LessonSessionTopicEntity> topics) {
        return new LessonSessionResult(
                lessonSession.id(),
                lessonSession.studentProgramId(),
                lessonSession.teacherId(),
                lessonSession.startedAt(),
                lessonSession.durationMinutes(),
                lessonSession.attendanceStatus(),
                lessonSession.summary(),
                lessonSession.privateNotes(),
                lessonSession.version(),
                lessonSession.createdAt(),
                lessonSession.updatedAt(),
                topics.stream()
                        .map(
                                topic ->
                                        new LessonSessionTopicResult(
                                                topic.topicId(),
                                                topic.primary(),
                                                topic.createdAt()))
                        .toList());
    }

    private SortParameters validateListParameters(int page, int size, String sort) {
        if (page < 0) {
            throw new InvalidSessionListParameterException(
                    "page", "must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new InvalidSessionListParameterException("size", "must be between 1 and 100");
        }
        String[] sortParts = sort.split(",", -1);
        if (sortParts.length != 2 || !ALLOWED_SORT_FIELDS.contains(sortParts[0])) {
            throw new InvalidSessionListParameterException(
                    "sort", "must use startedAt, createdAt, or durationMinutes");
        }
        if (!sortParts[1].equals("asc") && !sortParts[1].equals("desc")) {
            throw new InvalidSessionListParameterException("sort", "direction must be asc or desc");
        }
        return new SortParameters(sortParts[0], sortParts[1].equals("asc"));
    }

    private record SortParameters(String field, boolean ascending) {}
}
