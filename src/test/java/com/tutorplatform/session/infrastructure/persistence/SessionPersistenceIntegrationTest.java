package com.tutorplatform.session.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.tutorplatform.program.domain.*;
import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import com.tutorplatform.program.domain.studentprogram.StudentProgramEntity;
import com.tutorplatform.program.domain.studentprogram.StudentProgramRepository;
import com.tutorplatform.program.domain.studentprogram.StudentProgramStatus;
import com.tutorplatform.program.infrastructure.persistence.JpaModuleRepository;
import com.tutorplatform.program.infrastructure.persistence.JpaTopicRepository;
import com.tutorplatform.program.infrastructure.persistence.learningprogram.JpaLearningProgramRepository;
import com.tutorplatform.program.infrastructure.persistence.studentprogram.JpaStudentProgramRepository;
import com.tutorplatform.session.domain.*;
import com.tutorplatform.student.domain.StudentEntity;
import com.tutorplatform.student.domain.StudentRepository;
import com.tutorplatform.student.domain.StudentStatus;
import com.tutorplatform.student.infrastructure.persistence.JpaStudentRepository;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkEntity;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkRepository;
import com.tutorplatform.subject.domain.SubjectEntity;
import com.tutorplatform.subject.domain.SubjectRepository;
import com.tutorplatform.subject.domain.SubjectStatus;
import com.tutorplatform.subject.infrastructure.persistence.JpaSubjectRepository;
import com.tutorplatform.test.PostgresIntegrationTest;
import com.tutorplatform.user.domain.*;
import com.tutorplatform.user.infrastructure.persistence.JpaTeacherRepository;
import com.tutorplatform.user.infrastructure.persistence.JpaUserRepository;
import jakarta.persistence.OptimisticLockException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    JpaUserRepository.class,
    JpaTeacherRepository.class,
    JpaStudentRepository.class,
    JpaSubjectRepository.class,
    JpaLearningProgramRepository.class,
    JpaStudentProgramRepository.class,
    JpaModuleRepository.class,
    JpaTopicRepository.class,
    JpaLessonSessionRepository.class,
    JpaLessonSessionTopicRepository.class
})
class SessionPersistenceIntegrationTest extends PostgresIntegrationTest {

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_session_persistence", "008");
    }

    @Autowired private UserRepository userRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherStudentLinkRepository teacherStudentLinkRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private LearningProgramRepository learningProgramRepository;
    @Autowired private StudentProgramRepository studentProgramRepository;
    @Autowired private ModuleRepository moduleRepository;
    @Autowired private TopicRepository topicRepository;
    @Autowired private LessonSessionRepository lessonSessionRepository;
    @Autowired private LessonSessionTopicRepository lessonSessionTopicRepository;
    @Autowired private Flyway flyway;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void flywayV006AppliesSuccessfully() {
        assertThat(
                        Arrays.stream(flyway.info().applied())
                                .map(migration -> migration.getVersion().toString()))
                .containsExactly("001", "002", "003", "004", "005", "006", "007", "008");

        assertThat(
                        jdbcTemplate.queryForObject(
                                """
                select count(*)
                from information_schema.tables
                where table_schema = 'public'
                  and table_name in (
                    'lesson_sessions', 'lesson_session_topics', 'teacher_assessments'
                  )
                """,
                                Integer.class))
                .isEqualTo(3);
    }

    @Test
    void lessonSessionIsSavedAndListed() {
        SessionFixture fixture =
                createSessionFixture("session-save@example.com", AttendanceStatus.ATTENDED);

        LessonSessionEntity persisted =
                lessonSessionRepository.findById(fixture.lessonSession().id()).orElseThrow();

        assertThat(persisted.durationMinutes()).isEqualTo(60);
        assertThat(persisted.summary()).isEqualTo("Разобрали тему");
        assertThat(persisted.privateNotes()).isEqualTo("Заметка преподавателя");
        assertThat(persisted.version()).isZero();
        assertThat(persisted.createdAt()).isNotNull();
        assertThat(persisted.updatedAt()).isNotNull();
        assertThat(lessonSessionRepository.findAll())
                .extracting(LessonSessionEntity::id)
                .contains(fixture.lessonSession().id());
    }

    @Test
    void attendedStatusIsSaved() {
        assertStatusSaved(AttendanceStatus.ATTENDED, "attended@example.com");
    }

    @Test
    void missedStatusIsSaved() {
        assertStatusSaved(AttendanceStatus.MISSED, "missed@example.com");
    }

    @Test
    void cancelledStatusIsSaved() {
        assertStatusSaved(AttendanceStatus.CANCELLED, "cancelled@example.com");
    }

    @Test
    void zeroDurationIsRejectedByDatabase() {
        SessionFixture fixture =
                createSessionFixture("zero-duration@example.com", AttendanceStatus.ATTENDED);

        assertThatThrownBy(() -> insertSessionWithDuration(fixture, 0))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void durationAboveMaximumIsRejectedByDatabase() {
        SessionFixture fixture =
                createSessionFixture("long-duration@example.com", AttendanceStatus.ATTENDED);

        assertThatThrownBy(() -> insertSessionWithDuration(fixture, 601))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void lessonSessionIsLinkedToStudentProgram() {
        SessionFixture fixture =
                createSessionFixture("program-link@example.com", AttendanceStatus.ATTENDED);

        assertThat(lessonSessionRepository.findById(fixture.lessonSession().id()))
                .get()
                .extracting(LessonSessionEntity::studentProgramId)
                .isEqualTo(fixture.studentProgram().id());
    }

    @Test
    void lessonSessionRejectsUnknownStudentProgramForeignKey() {
        SessionFixture fixture =
                createSessionFixture(
                        "unknown-session-program@example.com", AttendanceStatus.ATTENDED);

        assertThatThrownBy(
                        () ->
                                jdbcTemplate.update(
                                        """
                insert into lesson_sessions (
                    id, student_program_id, teacher_id, started_at,
                    duration_minutes, attendance_status
                ) values (?, ?, ?, ?, 60, 'ATTENDED')
                """,
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        fixture.teacher().id(),
                                        Timestamp.from(Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void lessonSessionTopicIsLinkedToTopic() {
        SessionFixture fixture =
                createSessionFixture("topic-link@example.com", AttendanceStatus.ATTENDED);
        LessonSessionTopicEntity link =
                lessonSessionTopicRepository.saveAndFlush(
                        new LessonSessionTopicEntity(
                                fixture.lessonSession().id(), fixture.topic().id(), true));

        assertThat(link.createdAt()).isNotNull();
        assertThat(
                        lessonSessionTopicRepository.findAllByLessonSessionId(
                                fixture.lessonSession().id()))
                .singleElement()
                .satisfies(
                        persisted -> {
                            assertThat(persisted.topicId()).isEqualTo(fixture.topic().id());
                            assertThat(persisted.primary()).isTrue();
                        });
    }

    @Test
    void lessonSessionTopicRejectsUnknownTopicForeignKey() {
        SessionFixture fixture =
                createSessionFixture(
                        "unknown-session-topic@example.com", AttendanceStatus.ATTENDED);

        assertThatThrownBy(
                        () ->
                                jdbcTemplate.update(
                                        """
                insert into lesson_session_topics (lesson_session_id, topic_id)
                values (?, ?)
                """,
                                        fixture.lessonSession().id(),
                                        UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateLessonSessionTopicIsRejected() {
        SessionFixture fixture =
                createSessionFixture("duplicate-topic-link@example.com", AttendanceStatus.ATTENDED);
        lessonSessionTopicRepository.saveAndFlush(
                new LessonSessionTopicEntity(
                        fixture.lessonSession().id(), fixture.topic().id(), false));

        assertThatThrownBy(
                        () ->
                                jdbcTemplate.update(
                                        """
                insert into lesson_session_topics (lesson_session_id, topic_id)
                values (?, ?)
                """,
                                        fixture.lessonSession().id(),
                                        fixture.topic().id()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingLessonSessionCascadesToTopics() {
        SessionFixture fixture =
                createSessionFixture("session-cascade@example.com", AttendanceStatus.ATTENDED);
        lessonSessionTopicRepository.saveAndFlush(
                new LessonSessionTopicEntity(
                        fixture.lessonSession().id(), fixture.topic().id(), false));

        jdbcTemplate.update(
                "delete from lesson_sessions where id = ?", fixture.lessonSession().id());

        assertThat(
                        jdbcTemplate.queryForObject(
                                "select count(*) from lesson_session_topics where lesson_session_id = ?",
                                Integer.class,
                                fixture.lessonSession().id()))
                .isZero();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void lessonSessionOptimisticLockingWorks() {
        SessionFixture fixture =
                createSessionFixture("session-lock@example.com", AttendanceStatus.ATTENDED);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        LessonSessionEntity first =
                transaction.execute(
                        status ->
                                lessonSessionRepository
                                        .findById(fixture.lessonSession().id())
                                        .orElseThrow());
        LessonSessionEntity stale =
                transaction.execute(
                        status ->
                                lessonSessionRepository
                                        .findById(fixture.lessonSession().id())
                                        .orElseThrow());

        transaction.executeWithoutResult(
                status ->
                        lessonSessionRepository.saveAndFlush(
                                copyWithSummary(first, "Первая версия")));
        Throwable thrown =
                catchThrowable(
                        () ->
                                transaction.executeWithoutResult(
                                        status ->
                                                lessonSessionRepository.saveAndFlush(
                                                        copyWithSummary(
                                                                stale, "Устаревшая версия"))));

        assertThat(thrown).isNotNull();
        assertThat(hasOptimisticLockCause(thrown)).isTrue();
        assertThat(lessonSessionRepository.findById(fixture.lessonSession().id()))
                .get()
                .extracting(LessonSessionEntity::summary)
                .isEqualTo("Первая версия");
    }

    private void assertStatusSaved(AttendanceStatus status, String email) {
        SessionFixture fixture = createSessionFixture(email, status);
        assertThat(lessonSessionRepository.findById(fixture.lessonSession().id()))
                .get()
                .extracting(LessonSessionEntity::attendanceStatus)
                .isEqualTo(status);
    }

    private SessionFixture createSessionFixture(String email, AttendanceStatus status) {
        UserEntity user =
                new UserEntity(UUID.randomUUID(), email, "password-hash", UserStatus.ACTIVE);
        user.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(user);
        TeacherEntity teacher =
                teacherRepository.saveAndFlush(
                        new TeacherEntity(UUID.randomUUID(), user, "Teacher"));
        StudentEntity student =
                studentRepository.saveAndFlush(
                        new StudentEntity(UUID.randomUUID(), "Ученик", null, StudentStatus.ACTIVE));
        teacherStudentLinkRepository.saveAndFlush(new TeacherStudentLinkEntity(teacher, student));
        SubjectEntity subject =
                subjectRepository.saveAndFlush(
                        new SubjectEntity(
                                UUID.randomUUID(),
                                teacher.id(),
                                null,
                                "Предмет " + UUID.randomUUID(),
                                null,
                                SubjectStatus.ACTIVE));
        LearningProgramEntity learningProgram =
                learningProgramRepository.saveAndFlush(
                        new LearningProgramEntity(
                                UUID.randomUUID(),
                                teacher.id(),
                                subject.id(),
                                "Программа",
                                null,
                                LearningProgramStatus.DRAFT));
        StudentProgramEntity studentProgram =
                studentProgramRepository.saveAndFlush(
                        new StudentProgramEntity(
                                UUID.randomUUID(),
                                student.getId(),
                                learningProgram.getId(),
                                teacher.id(),
                                StudentProgramStatus.ACTIVE,
                                480,
                                Instant.now(),
                                null));
        ModuleEntity module =
                moduleRepository.saveAndFlush(
                        new ModuleEntity(
                                UUID.randomUUID(), learningProgram.getId(), "Модуль", null, 0));
        TopicEntity topic =
                topicRepository.saveAndFlush(
                        new TopicEntity(
                                UUID.randomUUID(),
                                module.id(),
                                "Тема",
                                null,
                                0,
                                TopicStatus.DRAFT));
        LessonSessionEntity lessonSession =
                lessonSessionRepository.saveAndFlush(
                        new LessonSessionEntity(
                                UUID.randomUUID(),
                                studentProgram.id(),
                                teacher.id(),
                                Instant.now(),
                                60,
                                status,
                                "Разобрали тему",
                                "Заметка преподавателя"));
        return new SessionFixture(teacher, studentProgram, topic, lessonSession);
    }

    private void insertSessionWithDuration(SessionFixture fixture, int durationMinutes) {
        jdbcTemplate.update(
                """
                insert into lesson_sessions (
                    id, student_program_id, teacher_id, started_at,
                    duration_minutes, attendance_status
                ) values (?, ?, ?, ?, ?, 'ATTENDED')
                """,
                UUID.randomUUID(),
                fixture.studentProgram().id(),
                fixture.teacher().id(),
                Timestamp.from(Instant.now()),
                durationMinutes);
    }

    private LessonSessionEntity copyWithSummary(LessonSessionEntity source, String summary) {
        return new LessonSessionEntity(
                source.id(),
                source.studentProgramId(),
                source.teacherId(),
                source.startedAt(),
                source.durationMinutes(),
                source.attendanceStatus(),
                summary,
                source.privateNotes(),
                source.version(),
                source.createdAt(),
                source.updatedAt());
    }

    private boolean hasOptimisticLockCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof OptimisticLockException
                    || current instanceof StaleObjectStateException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private record SessionFixture(
            TeacherEntity teacher,
            StudentProgramEntity studentProgram,
            TopicEntity topic,
            LessonSessionEntity lessonSession) {}
}
