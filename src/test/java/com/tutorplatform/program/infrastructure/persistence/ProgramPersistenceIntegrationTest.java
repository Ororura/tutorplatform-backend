package com.tutorplatform.program.infrastructure.persistence;

import com.tutorplatform.program.application.ProgramQuery;
import com.tutorplatform.program.application.ProgramQueryService;
import com.tutorplatform.program.domain.*;
import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import com.tutorplatform.program.domain.studentprogram.*;
import com.tutorplatform.program.infrastructure.persistence.learningprogram.JpaLearningProgramRepository;
import com.tutorplatform.program.infrastructure.persistence.studentprogram.JpaStudentProgramRepository;
import com.tutorplatform.program.infrastructure.persistence.studentprogram.JpaStudentTopicProgressRepository;
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
import com.tutorplatform.user.domain.*;
import com.tutorplatform.user.infrastructure.persistence.JpaTeacherRepository;
import com.tutorplatform.user.infrastructure.persistence.JpaUserRepository;
import jakarta.persistence.OptimisticLockException;
import org.flywaydb.core.Flyway;
import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
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
    JpaStudentTopicProgressRepository.class,
    ProgramQueryService.class
})
class ProgramPersistenceIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
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
    private StudentTopicProgressRepository studentTopicProgressRepository;
    @Autowired
    private ProgramQuery programQuery;
    @Autowired
    private Flyway flyway;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.target", () -> "008");
    }

    @Test
    void flywayMigrationsThroughV006ApplySuccessfully() {
        assertThat(Arrays.stream(flyway.info().applied())
            .map(migration -> migration.getVersion().toString()))
            .containsExactly("001", "002", "003", "004", "005", "006", "007", "008");

        assertThat(jdbcTemplate.queryForObject(
            """
                select count(*)
                from information_schema.tables
                where table_schema = 'public'
                  and table_name in (
                    'subjects', 'learning_programs', 'student_programs',
                    'modules', 'topics', 'student_topic_progress'
                  )
                """,
            Integer.class
        )).isEqualTo(6);
    }

    @Test
    void learningProgramBelongsToTeacher() {
        ProgramFixture fixture = createProgramFixture("teacher-owner@example.com");

        LearningProgramEntity persisted = learningProgramRepository.findById(fixture.learningProgram().getId())
            .orElseThrow();

        assertThat(persisted.getTeacherId()).isEqualTo(fixture.teacher().id());
        assertThat(persisted.getSubjectId()).isEqualTo(fixture.subject().id());
    }

    @Test
    void studentProgramLinksStudentAndLearningProgram() {
        ProgramFixture fixture = createProgramFixture("student-program@example.com");
        StudentEntity student = createStudent(fixture.teacher(), "Анна");
        StudentProgramEntity studentProgram = createStudentProgram(fixture, student);

        StudentProgramEntity persisted = studentProgramRepository.findById(studentProgram.id()).orElseThrow();

        assertThat(persisted.studentId()).isEqualTo(student.getId());
        assertThat(persisted.learningProgramId()).isEqualTo(fixture.learningProgram().getId());
        assertThat(persisted.assignedByTeacherId()).isEqualTo(fixture.teacher().id());
    }

    @Test
    void studentProgramForAnotherStudentIsDifferent() {
        ProgramFixture fixture = createProgramFixture("different-student@example.com");
        StudentEntity firstStudent = createStudent(fixture.teacher(), "Первый");
        StudentEntity secondStudent = createStudent(fixture.teacher(), "Второй");
        StudentProgramEntity firstProgram = createStudentProgram(fixture, firstStudent);
        StudentProgramEntity secondProgram = createStudentProgram(fixture, secondStudent);

        ProgramQuery.StudentProgramContext first = programQuery.findStudentProgram(firstProgram.id()).orElseThrow();
        ProgramQuery.StudentProgramContext second = programQuery.findStudentProgram(secondProgram.id()).orElseThrow();

        assertThat(first.belongsToStudent(firstStudent.getId())).isTrue();
        assertThat(first.belongsToStudent(secondStudent.getId())).isFalse();
        assertThat(second.belongsToStudent(secondStudent.getId())).isTrue();
    }

    @Test
    void moduleBelongsToLearningProgram() {
        ProgramFixture fixture = createProgramFixture("module-owner@example.com");
        ModuleEntity module = createModule(fixture.learningProgram(), 0);

        assertThat(moduleRepository.findById(module.id()).orElseThrow().learningProgramId())
            .isEqualTo(fixture.learningProgram().getId());
    }

    @Test
    void topicBelongsToModule() {
        ProgramFixture fixture = createProgramFixture("topic-module@example.com");
        ModuleEntity module = createModule(fixture.learningProgram(), 0);
        TopicEntity topic = createTopic(module, 0);

        assertThat(topicRepository.findById(topic.id()).orElseThrow().moduleId())
            .isEqualTo(module.id());
    }

    @Test
    void topicLearningProgramIsResolvedThroughModule() {
        ProgramFixture fixture = createProgramFixture("topic-path@example.com");
        ProgramFixture otherFixture = createProgramFixture("other-topic-path@example.com");
        ModuleEntity module = createModule(fixture.learningProgram(), 0);
        TopicEntity topic = createTopic(module, 0);

        assertThat(programQuery.topicBelongsToLearningProgram(topic.id(), fixture.learningProgram().getId()))
            .isTrue();
        assertThat(programQuery.topicBelongsToLearningProgram(topic.id(), otherFixture.learningProgram().getId()))
            .isFalse();
    }

    @Test
    void duplicateModulePositionIsRejected() {
        ProgramFixture fixture = createProgramFixture("duplicate-module@example.com");
        createModule(fixture.learningProgram(), 0);

        assertThatThrownBy(() -> createModule(fixture.learningProgram(), 0))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateTopicPositionIsRejected() {
        ProgramFixture fixture = createProgramFixture("duplicate-topic@example.com");
        ModuleEntity module = createModule(fixture.learningProgram(), 0);
        createTopic(module, 0);

        assertThatThrownBy(() -> createTopic(module, 0))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void studentTopicProgressCompositePrimaryKeyWorks() {
        ProgramFixture fixture = createProgramFixture("progress-key@example.com");
        StudentEntity firstStudent = createStudent(fixture.teacher(), "Первый");
        StudentEntity secondStudent = createStudent(fixture.teacher(), "Второй");
        StudentProgramEntity firstStudentProgram = createStudentProgram(fixture, firstStudent);
        StudentProgramEntity secondStudentProgram = createStudentProgram(fixture, secondStudent);
        TopicEntity topic = createTopic(createModule(fixture.learningProgram(), 0), 0);

        studentTopicProgressRepository.saveAndFlush(new StudentTopicProgressEntity(
            firstStudentProgram.id(), topic.id(), StudentTopicProgressStatus.AVAILABLE, null, null
        ));
        studentTopicProgressRepository.saveAndFlush(new StudentTopicProgressEntity(
            secondStudentProgram.id(), topic.id(), StudentTopicProgressStatus.LOCKED, null, null
        ));

        assertThat(studentTopicProgressRepository.findById(firstStudentProgram.id(), topic.id()))
            .get().extracting(StudentTopicProgressEntity::status)
            .isEqualTo(StudentTopicProgressStatus.AVAILABLE);
        assertThat(studentTopicProgressRepository.findById(secondStudentProgram.id(), topic.id()))
            .get().extracting(StudentTopicProgressEntity::status)
            .isEqualTo(StudentTopicProgressStatus.LOCKED);
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from student_topic_progress where topic_id = ?",
            Integer.class,
            topic.id()
        )).isEqualTo(2);
    }

    @Test
    void nonPositiveReportIntervalIsRejected() {
        ProgramFixture fixture = createProgramFixture("invalid-interval@example.com");
        StudentEntity student = createStudent(fixture.teacher(), "Интервал");

        assertThatThrownBy(() -> jdbcTemplate.update(
            """
                insert into student_programs (
                    id, student_id, learning_program_id, assigned_by_teacher_id,
                    status, report_interval_minutes
                ) values (?, ?, ?, ?, 'ACTIVE', 0)
                """,
            UUID.randomUUID(),
            student.getId(),
            fixture.learningProgram().getId(),
            fixture.teacher().id()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void optimisticLockingWorksForAllVersionedEntities() {
        ProgramFixture fixture = createProgramFixture("optimistic-lock@example.com");
        StudentEntity student = createStudent(fixture.teacher(), "Версия");
        StudentProgramEntity studentProgram = createStudentProgram(fixture, student);
        ModuleEntity module = createModule(fixture.learningProgram(), 0);
        TopicEntity topic = createTopic(module, 0);

        assertOptimisticLocking(
            fixture.learningProgram().getId(),
            learningProgramRepository::findById,
            learningProgramRepository::saveAndFlush,
            value -> {
                value.update("Первая версия", value.getDescription(), value.getStatus());
                return value;
            },
            value -> {
                value.update("Устаревшая версия", value.getDescription(), value.getStatus());
                return value;
            },
            "LearningProgramEntity"
        );
        assertThat(learningProgramRepository.findById(fixture.learningProgram().getId()))
            .get().extracting(LearningProgramEntity::getTitle)
            .isEqualTo("Первая версия");

        assertOptimisticLocking(
            studentProgram.id(),
            studentProgramRepository::findById,
            studentProgramRepository::saveAndFlush,
            value -> copyWithStatus(value, StudentProgramStatus.PAUSED),
            value -> copyWithStatus(value, StudentProgramStatus.COMPLETED),
            "StudentProgramEntity"
        );
        assertThat(studentProgramRepository.findById(studentProgram.id()))
            .get().extracting(StudentProgramEntity::status)
            .isEqualTo(StudentProgramStatus.PAUSED);

        assertOptimisticLocking(
            topic.id(),
            topicRepository::findById,
            topicRepository::saveAndFlush,
            value -> copyWithTitle(value, "Первая версия"),
            value -> copyWithTitle(value, "Устаревшая версия"),
            "TopicEntity"
        );
        assertThat(topicRepository.findById(topic.id()))
            .get().extracting(TopicEntity::title)
            .isEqualTo("Первая версия");
    }

    private ProgramFixture createProgramFixture(String email) {
        TeacherEntity teacher = createTeacher(email);
        SubjectEntity subject = subjectRepository.saveAndFlush(new SubjectEntity(
            UUID.randomUUID(),
            teacher.id(),
            null,
            "Предмет " + UUID.randomUUID(),
            null,
            SubjectStatus.ACTIVE
        ));
        LearningProgramEntity learningProgram = learningProgramRepository.saveAndFlush(new LearningProgramEntity(
            UUID.randomUUID(),
            teacher.id(),
            subject.id(),
            "Программа",
            null,
            LearningProgramStatus.DRAFT
        ));
        return new ProgramFixture(teacher, subject, learningProgram);
    }

    private TeacherEntity createTeacher(String email) {
        UserEntity user = new UserEntity(UUID.randomUUID(), email, "password-hash", UserStatus.ACTIVE);
        user.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(user);
        return teacherRepository.saveAndFlush(new TeacherEntity(UUID.randomUUID(), user, "Teacher"));
    }

    private StudentEntity createStudent(TeacherEntity teacher, String firstName) {
        StudentEntity student = studentRepository.saveAndFlush(new StudentEntity(
            UUID.randomUUID(), firstName, null, StudentStatus.ACTIVE
        ));
        teacherStudentLinkRepository.saveAndFlush(new TeacherStudentLinkEntity(teacher, student));
        return student;
    }

    private StudentProgramEntity createStudentProgram(ProgramFixture fixture, StudentEntity student) {
        return studentProgramRepository.saveAndFlush(new StudentProgramEntity(
            UUID.randomUUID(),
            student.getId(),
            fixture.learningProgram().getId(),
            fixture.teacher().id(),
            StudentProgramStatus.ACTIVE,
            480,
            Instant.now(),
            null
        ));
    }

    private ModuleEntity createModule(LearningProgramEntity learningProgram, int position) {
        return moduleRepository.saveAndFlush(new ModuleEntity(
            UUID.randomUUID(), learningProgram.getId(), "Модуль", null, position
        ));
    }

    private TopicEntity createTopic(ModuleEntity module, int position) {
        return topicRepository.saveAndFlush(new TopicEntity(
            UUID.randomUUID(), module.id(), "Тема", null, position, TopicStatus.DRAFT
        ));
    }

    private StudentProgramEntity copyWithStatus(StudentProgramEntity source, StudentProgramStatus status) {
        return new StudentProgramEntity(
            source.id(), source.studentId(), source.learningProgramId(), source.assignedByTeacherId(),
            status, source.reportIntervalMinutes(), source.startedAt(), source.completedAt(),
            source.version(), source.createdAt(), source.updatedAt()
        );
    }

    private TopicEntity copyWithTitle(TopicEntity source, String title) {
        return new TopicEntity(
            source.id(), source.moduleId(), title, source.description(), source.position(),
            source.status(), source.version(), source.createdAt(), source.updatedAt()
        );
    }

    private <T> void assertOptimisticLocking(
        UUID id,
        Function<UUID, Optional<T>> findById,
        Function<T, T> saveAndFlush,
        UnaryOperator<T> firstChange,
        UnaryOperator<T> staleChange,
        String entityName
    ) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        T first = transaction.execute(status -> findById.apply(id).orElseThrow());
        T stale = transaction.execute(status -> findById.apply(id).orElseThrow());

        transaction.executeWithoutResult(status -> saveAndFlush.apply(firstChange.apply(first)));

        Throwable thrown = catchThrowable(() -> transaction.executeWithoutResult(
            status -> saveAndFlush.apply(staleChange.apply(stale))
        ));
        assertThat(thrown).as("stale %s update", entityName).isNotNull();
        assertThat(hasOptimisticLockCause(thrown))
            .as("%s must fail specifically because of optimistic locking", entityName)
            .isTrue();
    }

    private boolean hasOptimisticLockCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof OptimisticLockException || current instanceof StaleObjectStateException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private record ProgramFixture(
        TeacherEntity teacher,
        SubjectEntity subject,
        LearningProgramEntity learningProgram
    ) {
    }
}
