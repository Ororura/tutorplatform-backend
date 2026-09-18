package com.tutorplatform.session.application;

import com.tutorplatform.test.PostgresIntegrationTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.domain.*;
import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import com.tutorplatform.program.domain.studentprogram.StudentProgramEntity;
import com.tutorplatform.program.domain.studentprogram.StudentProgramRepository;
import com.tutorplatform.program.domain.studentprogram.StudentProgramStatus;
import com.tutorplatform.report.application.LearningPeriodService;
import com.tutorplatform.report.domain.LearningPeriodStatus;
import com.tutorplatform.session.application.exception.InvalidLessonSessionTopicsException;
import com.tutorplatform.session.application.exception.StudentProgramNotFoundException;
import com.tutorplatform.session.application.exception.TopicOutsideStudentProgramException;
import com.tutorplatform.session.domain.AttendanceStatus;
import com.tutorplatform.session.domain.LessonSessionRepository;
import com.tutorplatform.session.domain.LessonSessionTopicRepository;
import com.tutorplatform.student.application.management.StudentNotFoundException;
import com.tutorplatform.student.domain.StudentEntity;
import com.tutorplatform.student.domain.StudentRepository;
import com.tutorplatform.student.domain.StudentStatus;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkEntity;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkRepository;
import com.tutorplatform.subject.domain.SubjectEntity;
import com.tutorplatform.subject.domain.SubjectRepository;
import com.tutorplatform.subject.domain.SubjectStatus;
import com.tutorplatform.user.domain.*;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SessionApplicationIntegrationTest extends PostgresIntegrationTest {

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_session_application", "008");
    }

    @Autowired
    private LessonSessionService lessonSessionService;
    @Autowired
    private LessonSessionRepository lessonSessionRepository;
    @Autowired
    private LessonSessionTopicRepository lessonSessionTopicRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private TeacherStudentLinkRepository teacherStudentLinkRepository;
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private LearningProgramRepository learningProgramRepository;
    @Autowired
    private StudentProgramRepository studentProgramRepository;
    @Autowired
    private ModuleRepository moduleRepository;
    @Autowired
    private TopicRepository topicRepository;
    @Autowired
    private LearningPeriodService learningPeriodService;

    @Test
    void createsLessonSessionWithServerTeacherAndTopics() {
        SessionFixture fixture = createFixture("create-session@example.com", "Ученик");

        LessonSessionResult created = createSession(
            fixture,
            AttendanceStatus.ATTENDED,
            List.of(new LessonSessionTopicInput(fixture.firstTopic().id(), true))
        );

        assertThat(created.teacherId()).isEqualTo(fixture.teacher().id());
        assertThat(created.studentProgramId()).isEqualTo(fixture.studentProgram().id());
        assertThat(created.durationMinutes()).isEqualTo(60);
        assertThat(created.attendanceStatus()).isEqualTo(AttendanceStatus.ATTENDED);
        assertThat(created.topics()).singleElement().satisfies(topic -> {
            assertThat(topic.topicId()).isEqualTo(fixture.firstTopic().id());
            assertThat(topic.primary()).isTrue();
        });
        assertThat(learningPeriodService.listLearningPeriods(fixture.studentProgram().id()))
            .singleElement()
            .satisfies(period -> {
                assertThat(period.status()).isEqualTo(LearningPeriodStatus.ACTIVE);
                assertThat(period.startedAt()).isEqualTo(created.startedAt());
            });
    }

    @Test
    void rejectsStudentOwnedByAnotherTeacher() {
        SessionFixture owner = createFixture("owner@example.com", "Владелец");
        SessionFixture foreign = createFixture("foreign@example.com", "Чужой");

        assertThatThrownBy(() -> lessonSessionService.createLessonSession(
            owner.principal(),
            createCommand(foreign, List.of())
        )).isInstanceOf(StudentNotFoundException.class);
    }

    @Test
    void rejectsStudentProgramOfAnotherStudent() {
        TeacherFixture teacher = createTeacher("program-other-student@example.com");
        SessionFixture first = createFixture(teacher, "Первый");
        SessionFixture second = createFixture(teacher, "Второй");

        CreateLessonSessionCommand command = new CreateLessonSessionCommand(
            first.student().getId(),
            second.studentProgram().id(),
            Instant.now(),
            60,
            AttendanceStatus.ATTENDED,
            null,
            null,
            List.of()
        );

        assertThatThrownBy(() -> lessonSessionService.createLessonSession(teacher.principal(), command))
            .isInstanceOf(StudentProgramNotFoundException.class);
    }

    @Test
    void rejectsTopicFromAnotherLearningProgram() {
        TeacherFixture teacher = createTeacher("other-program-topic@example.com");
        SessionFixture first = createFixture(teacher, "Первый");
        SessionFixture second = createFixture(teacher, "Второй");

        assertThatThrownBy(() -> lessonSessionService.createLessonSession(
            teacher.principal(),
            createCommand(first, List.of(
                new LessonSessionTopicInput(second.firstTopic().id(), true)
            ))
        )).isInstanceOf(TopicOutsideStudentProgramException.class);
    }

    @Test
    void rejectsDuplicateTopic() {
        SessionFixture fixture = createFixture("duplicate-app-topic@example.com", "Ученик");
        UUID topicId = fixture.firstTopic().id();

        assertThatThrownBy(() -> lessonSessionService.createLessonSession(
            fixture.principal(),
            createCommand(fixture, List.of(
                new LessonSessionTopicInput(topicId, true),
                new LessonSessionTopicInput(topicId, false)
            ))
        )).isInstanceOf(InvalidLessonSessionTopicsException.class);
    }

    @Test
    void rejectsMoreThanOnePrimaryTopic() {
        SessionFixture fixture = createFixture("primary-app-topic@example.com", "Ученик");

        assertThatThrownBy(() -> lessonSessionService.createLessonSession(
            fixture.principal(),
            createCommand(fixture, List.of(
                new LessonSessionTopicInput(fixture.firstTopic().id(), true),
                new LessonSessionTopicInput(fixture.secondTopic().id(), true)
            ))
        )).isInstanceOf(InvalidLessonSessionTopicsException.class);
    }

    @Test
    void invalidTopicLeavesNoPartiallyCreatedSession() {
        TeacherFixture teacher = createTeacher("rollback-topic@example.com");
        SessionFixture target = createFixture(teacher, "Целевой");
        SessionFixture otherProgram = createFixture(teacher, "Другой");
        long before = lessonSessionRepository.findAll().size();

        assertThatThrownBy(() -> lessonSessionService.createLessonSession(
            teacher.principal(),
            createCommand(target, List.of(
                new LessonSessionTopicInput(target.firstTopic().id(), true),
                new LessonSessionTopicInput(otherProgram.firstTopic().id(), false)
            ))
        )).isInstanceOf(TopicOutsideStudentProgramException.class);

        assertThat(lessonSessionRepository.findAll()).hasSize(Math.toIntExact(before));
    }

    @Test
    void readsOwnedLessonSession() {
        SessionFixture fixture = createFixture("read-session@example.com", "Ученик");
        LessonSessionResult created = createSession(
            fixture,
            AttendanceStatus.MISSED,
            List.of(new LessonSessionTopicInput(fixture.firstTopic().id(), true))
        );

        LessonSessionResult found = lessonSessionService.getLessonSession(
            fixture.principal(), fixture.student().getId(), created.id()
        );

        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.attendanceStatus()).isEqualTo(AttendanceStatus.MISSED);
        assertThat(found.topics()).extracting(LessonSessionTopicResult::topicId)
            .containsExactly(fixture.firstTopic().id());
    }

    @Test
    void listsOnlyOwnedStudentSessionsWithDatabasePagination() {
        SessionFixture fixture = createFixture("list-session@example.com", "Целевой");
        SessionFixture other = createFixture(fixture.teacherFixture(), "Другой");
        createSession(fixture, AttendanceStatus.ATTENDED, List.of());
        createSession(fixture, AttendanceStatus.MISSED, List.of(
            new LessonSessionTopicInput(fixture.firstTopic().id(), false)
        ));
        createSession(fixture, AttendanceStatus.CANCELLED, List.of());
        createSession(other, AttendanceStatus.ATTENDED, List.of());

        LessonSessionPageResult firstPage = lessonSessionService.listLessonSessions(
            fixture.principal(), fixture.student().getId(), 0, 2
        );
        LessonSessionPageResult secondPage = lessonSessionService.listLessonSessions(
            fixture.principal(), fixture.student().getId(), 1, 2
        );

        assertThat(firstPage.items()).hasSize(2);
        assertThat(secondPage.items()).hasSize(1);
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.items()).allSatisfy(session ->
            assertThat(session.studentProgramId()).isEqualTo(fixture.studentProgram().id())
        );
    }

    @Test
    void updatesAllowedLessonSessionFields() {
        SessionFixture fixture = createFixture("update-session@example.com", "Ученик");
        LessonSessionResult created = createSession(fixture, AttendanceStatus.ATTENDED, List.of());
        Instant newStartedAt = created.startedAt().plusSeconds(3600);

        LessonSessionResult updated = lessonSessionService.updateLessonSession(
            fixture.principal(),
            fixture.student().getId(),
            created.id(),
            new UpdateLessonSessionCommand(
                newStartedAt,
                90,
                AttendanceStatus.CANCELLED,
                "Новый итог",
                "Новая заметка",
                created.version(),
                List.of()
            )
        );

        assertThat(updated.startedAt()).isEqualTo(newStartedAt);
        assertThat(updated.durationMinutes()).isEqualTo(90);
        assertThat(updated.attendanceStatus()).isEqualTo(AttendanceStatus.CANCELLED);
        assertThat(updated.summary()).isEqualTo("Новый итог");
        assertThat(updated.privateNotes()).isEqualTo("Новая заметка");
        assertThat(updated.teacherId()).isEqualTo(created.teacherId());
        assertThat(updated.studentProgramId()).isEqualTo(created.studentProgramId());
        assertThat(updated.version()).isEqualTo(created.version() + 1);
        assertThat(learningPeriodService.listLearningPeriods(fixture.studentProgram().id()))
            .singleElement()
            .satisfies(period -> assertThat(period.startedAt()).isNull());
    }

    @Test
    void replacesLessonSessionTopicsAtomically() {
        SessionFixture fixture = createFixture("update-topics@example.com", "Ученик");
        LessonSessionResult created = createSession(fixture, AttendanceStatus.ATTENDED, List.of(
            new LessonSessionTopicInput(fixture.firstTopic().id(), true)
        ));

        LessonSessionResult updated = lessonSessionService.updateLessonSession(
            fixture.principal(),
            fixture.student().getId(),
            created.id(),
            updateCommand(created, List.of(
                new LessonSessionTopicInput(fixture.secondTopic().id(), true)
            ))
        );

        assertThat(updated.topics()).extracting(LessonSessionTopicResult::topicId)
            .containsExactly(fixture.secondTopic().id());
        assertThat(lessonSessionTopicRepository.findAllByLessonSessionId(created.id()))
            .singleElement()
            .extracting(topic -> topic.topicId())
            .isEqualTo(fixture.secondTopic().id());
    }

    @Test
    void rejectsStaleUpdateWithOptimisticLockingAndKeepsTopics() {
        SessionFixture fixture = createFixture("app-optimistic-lock@example.com", "Ученик");
        LessonSessionResult created = createSession(fixture, AttendanceStatus.ATTENDED, List.of(
            new LessonSessionTopicInput(fixture.firstTopic().id(), true)
        ));
        UpdateLessonSessionCommand firstUpdate = updateCommand(created, List.of(
            new LessonSessionTopicInput(fixture.secondTopic().id(), true)
        ));
        UpdateLessonSessionCommand staleUpdate = updateCommand(created, List.of());

        LessonSessionResult updated = lessonSessionService.updateLessonSession(
            fixture.principal(), fixture.student().getId(), created.id(), firstUpdate
        );
        Throwable thrown = catchThrowable(() -> lessonSessionService.updateLessonSession(
            fixture.principal(), fixture.student().getId(), created.id(), staleUpdate
        ));

        assertThat(thrown).isNotNull();
        assertThat(hasOptimisticLockCause(thrown)).isTrue();
        LessonSessionResult persisted = lessonSessionService.getLessonSession(
            fixture.principal(), fixture.student().getId(), created.id()
        );
        assertThat(persisted.version()).isEqualTo(updated.version());
        assertThat(persisted.topics()).extracting(LessonSessionTopicResult::topicId)
            .containsExactly(fixture.secondTopic().id());
    }

    private LessonSessionResult createSession(
        SessionFixture fixture,
        AttendanceStatus status,
        List<LessonSessionTopicInput> topics
    ) {
        return lessonSessionService.createLessonSession(
            fixture.principal(),
            createCommand(fixture, topics, status)
        );
    }

    private CreateLessonSessionCommand createCommand(
        SessionFixture fixture,
        List<LessonSessionTopicInput> topics
    ) {
        return createCommand(fixture, topics, AttendanceStatus.ATTENDED);
    }

    private CreateLessonSessionCommand createCommand(
        SessionFixture fixture,
        List<LessonSessionTopicInput> topics,
        AttendanceStatus status
    ) {
        return new CreateLessonSessionCommand(
            fixture.student().getId(),
            fixture.studentProgram().id(),
            Instant.now(),
            60,
            status,
            "Итог",
            "Заметка",
            topics
        );
    }

    private UpdateLessonSessionCommand updateCommand(
        LessonSessionResult source,
        List<LessonSessionTopicInput> topics
    ) {
        return new UpdateLessonSessionCommand(
            source.startedAt(),
            source.durationMinutes(),
            source.attendanceStatus(),
            source.summary(),
            source.privateNotes(),
            source.version(),
            topics
        );
    }

    private SessionFixture createFixture(String email, String firstName) {
        return createFixture(createTeacher(email), firstName);
    }

    private SessionFixture createFixture(TeacherFixture teacherFixture, String firstName) {
        StudentEntity student = studentRepository.saveAndFlush(new StudentEntity(
            UUID.randomUUID(), firstName, null, StudentStatus.ACTIVE
        ));
        teacherStudentLinkRepository.saveAndFlush(new TeacherStudentLinkEntity(
            teacherFixture.teacher(), student
        ));
        SubjectEntity subject = subjectRepository.saveAndFlush(new SubjectEntity(
            UUID.randomUUID(),
            teacherFixture.teacher().id(),
            null,
            "Предмет " + UUID.randomUUID(),
            null,
            SubjectStatus.ACTIVE
        ));
        LearningProgramEntity learningProgram = learningProgramRepository.saveAndFlush(
            new LearningProgramEntity(
                UUID.randomUUID(),
                teacherFixture.teacher().id(),
                subject.id(),
                "Программа",
                null,
                LearningProgramStatus.DRAFT
            )
        );
        StudentProgramEntity studentProgram = studentProgramRepository.saveAndFlush(
            new StudentProgramEntity(
                UUID.randomUUID(),
                student.getId(),
                learningProgram.getId(),
                teacherFixture.teacher().id(),
                StudentProgramStatus.ACTIVE,
                480,
                Instant.now(),
                null
            )
        );
        ModuleEntity module = moduleRepository.saveAndFlush(new ModuleEntity(
            UUID.randomUUID(), learningProgram.getId(), "Модуль", null, 0
        ));
        TopicEntity firstTopic = topicRepository.saveAndFlush(new TopicEntity(
            UUID.randomUUID(), module.id(), "Первая тема", null, 0, TopicStatus.DRAFT
        ));
        TopicEntity secondTopic = topicRepository.saveAndFlush(new TopicEntity(
            UUID.randomUUID(), module.id(), "Вторая тема", null, 1, TopicStatus.DRAFT
        ));
        return new SessionFixture(
            teacherFixture,
            student,
            studentProgram,
            firstTopic,
            secondTopic
        );
    }

    private TeacherFixture createTeacher(String email) {
        UserEntity user = new UserEntity(UUID.randomUUID(), email, "password-hash", UserStatus.ACTIVE);
        user.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(user);
        TeacherEntity teacher = teacherRepository.saveAndFlush(new TeacherEntity(
            UUID.randomUUID(), user, "Teacher"
        ));
        AuthenticatedUser principal = new AuthenticatedUser(
            user.id(),
            email,
            "",
            true,
            List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );
        return new TeacherFixture(teacher, principal);
    }

    private boolean hasOptimisticLockCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof OptimisticLockException
                || current instanceof StaleObjectStateException
                || current instanceof ObjectOptimisticLockingFailureException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private record TeacherFixture(TeacherEntity teacher, AuthenticatedUser principal) {
    }

    private record SessionFixture(
        TeacherFixture teacherFixture,
        StudentEntity student,
        StudentProgramEntity studentProgram,
        TopicEntity firstTopic,
        TopicEntity secondTopic
    ) {
        TeacherEntity teacher() {
            return teacherFixture.teacher();
        }

        AuthenticatedUser principal() {
            return teacherFixture.principal();
        }
    }
}
