package com.tutorplatform.program.infrastructure.persistence;

import com.tutorplatform.program.application.ProgramQuery;
import com.tutorplatform.program.application.ProgramQueryService;
import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import com.tutorplatform.program.domain.ModuleEntity;
import com.tutorplatform.program.domain.ModuleRepository;
import com.tutorplatform.program.domain.studentprogram.StudentProgramEntity;
import com.tutorplatform.program.domain.studentprogram.StudentProgramRepository;
import com.tutorplatform.program.domain.studentprogram.StudentProgramStatus;
import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressEntity;
import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressRepository;
import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressStatus;
import com.tutorplatform.program.domain.TopicEntity;
import com.tutorplatform.program.domain.TopicRepository;
import com.tutorplatform.program.domain.TopicStatus;
import com.tutorplatform.program.infrastructure.persistence.studentprogram.JpaStudentProgramRepository;
import com.tutorplatform.program.infrastructure.persistence.studentprogram.JpaStudentTopicProgressRepository;
import com.tutorplatform.program.infrastructure.persistence.studentprogram.StudentProgramDatabaseModel;
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
import com.tutorplatform.user.domain.TeacherEntity;
import com.tutorplatform.user.domain.TeacherRepository;
import com.tutorplatform.user.domain.UserEntity;
import com.tutorplatform.user.domain.UserRepository;
import com.tutorplatform.user.domain.UserRole;
import com.tutorplatform.user.domain.UserStatus;
import com.tutorplatform.user.infrastructure.persistence.JpaTeacherRepository;
import com.tutorplatform.user.infrastructure.persistence.JpaUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

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

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.target", () -> "003");
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
    @Autowired private StudentTopicProgressRepository studentTopicProgressRepository;
    @Autowired private ProgramQuery programQuery;
    @Autowired private Flyway flyway;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @Test
    void flywayV003AppliesSuccessfully() {
        assertThat(Arrays.stream(flyway.info().applied())
                .map(migration -> migration.getVersion().toString()))
                .containsExactly("001", "002", "003");

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

        assertThat(persisted.getTeacherId()).isEqualTo(fixture.teacher().getId());
        assertThat(persisted.getSubjectId()).isEqualTo(fixture.subject().getId());
    }

    @Test
    void studentProgramLinksStudentAndLearningProgram() {
        ProgramFixture fixture = createProgramFixture("student-program@example.com");
        StudentEntity student = createStudent(fixture.teacher(), "Анна");
        StudentProgramEntity studentProgram = createStudentProgram(fixture, student);

        StudentProgramEntity persisted = studentProgramRepository.findById(studentProgram.getId()).orElseThrow();

        assertThat(persisted.getStudentId()).isEqualTo(student.getId());
        assertThat(persisted.getLearningProgramId()).isEqualTo(fixture.learningProgram().getId());
        assertThat(persisted.getAssignedByTeacherId()).isEqualTo(fixture.teacher().getId());
    }

    @Test
    void studentProgramForAnotherStudentIsDifferent() {
        ProgramFixture fixture = createProgramFixture("different-student@example.com");
        StudentEntity firstStudent = createStudent(fixture.teacher(), "Первый");
        StudentEntity secondStudent = createStudent(fixture.teacher(), "Второй");
        StudentProgramEntity firstProgram = createStudentProgram(fixture, firstStudent);
        StudentProgramEntity secondProgram = createStudentProgram(fixture, secondStudent);

        ProgramQuery.StudentProgramContext first = programQuery.findStudentProgram(firstProgram.getId()).orElseThrow();
        ProgramQuery.StudentProgramContext second = programQuery.findStudentProgram(secondProgram.getId()).orElseThrow();

        assertThat(first.belongsToStudent(firstStudent.getId())).isTrue();
        assertThat(first.belongsToStudent(secondStudent.getId())).isFalse();
        assertThat(second.belongsToStudent(secondStudent.getId())).isTrue();
    }

    @Test
    void moduleBelongsToLearningProgram() {
        ProgramFixture fixture = createProgramFixture("module-owner@example.com");
        ModuleEntity module = createModule(fixture.learningProgram(), 0);

        assertThat(moduleRepository.findById(module.getId()).orElseThrow().getLearningProgramId())
                .isEqualTo(fixture.learningProgram().getId());
    }

    @Test
    void topicBelongsToModule() {
        ProgramFixture fixture = createProgramFixture("topic-module@example.com");
        ModuleEntity module = createModule(fixture.learningProgram(), 0);
        TopicEntity topic = createTopic(module, 0);

        assertThat(topicRepository.findById(topic.getId()).orElseThrow().getModuleId())
                .isEqualTo(module.getId());
    }

    @Test
    void topicLearningProgramIsResolvedThroughModule() {
        ProgramFixture fixture = createProgramFixture("topic-path@example.com");
        ProgramFixture otherFixture = createProgramFixture("other-topic-path@example.com");
        ModuleEntity module = createModule(fixture.learningProgram(), 0);
        TopicEntity topic = createTopic(module, 0);

        assertThat(programQuery.topicBelongsToLearningProgram(topic.getId(), fixture.learningProgram().getId()))
                .isTrue();
        assertThat(programQuery.topicBelongsToLearningProgram(topic.getId(), otherFixture.learningProgram().getId()))
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
                firstStudentProgram.getId(), topic.getId(), StudentTopicProgressStatus.AVAILABLE, null, null
        ));
        studentTopicProgressRepository.saveAndFlush(new StudentTopicProgressEntity(
                secondStudentProgram.getId(), topic.getId(), StudentTopicProgressStatus.LOCKED, null, null
        ));

        assertThat(studentTopicProgressRepository.findById(firstStudentProgram.getId(), topic.getId()))
                .get().extracting(StudentTopicProgressEntity::getStatus)
                .isEqualTo(StudentTopicProgressStatus.AVAILABLE);
        assertThat(studentTopicProgressRepository.findById(secondStudentProgram.getId(), topic.getId()))
                .get().extracting(StudentTopicProgressEntity::getStatus)
                .isEqualTo(StudentTopicProgressStatus.LOCKED);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from student_topic_progress where topic_id = ?",
                Integer.class,
                topic.getId()
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
                fixture.teacher().getId()
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
                LearningProgramDatabaseModel.class,
                fixture.learningProgram().getId(),
                model -> {
                    LearningProgramEntity value = model.toEntity();
                    value.update("Первая версия", value.getDescription(), value.getStatus());
                    model.updateFrom(value);
                },
                model -> {
                    LearningProgramEntity value = model.toEntity();
                    value.update("Устаревшая версия", value.getDescription(), value.getStatus());
                    model.updateFrom(value);
                }
        );
        assertOptimisticLocking(
                StudentProgramDatabaseModel.class,
                studentProgram.getId(),
                model -> model.updateFrom(copyWithStatus(model.toEntity(), StudentProgramStatus.PAUSED)),
                model -> model.updateFrom(copyWithStatus(model.toEntity(), StudentProgramStatus.COMPLETED))
        );
        assertOptimisticLocking(
                TopicDatabaseModel.class,
                topic.getId(),
                model -> model.updateFrom(copyWithTitle(model.toEntity(), "Первая версия")),
                model -> model.updateFrom(copyWithTitle(model.toEntity(), "Устаревшая версия"))
        );
    }

    private ProgramFixture createProgramFixture(String email) {
        TeacherEntity teacher = createTeacher(email);
        SubjectEntity subject = subjectRepository.saveAndFlush(new SubjectEntity(
                UUID.randomUUID(),
                teacher.getId(),
                null,
                "Предмет " + UUID.randomUUID(),
                null,
                SubjectStatus.ACTIVE
        ));
        LearningProgramEntity learningProgram = learningProgramRepository.saveAndFlush(new LearningProgramEntity(
                UUID.randomUUID(),
                teacher.getId(),
                subject.getId(),
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
                fixture.teacher().getId(),
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
                UUID.randomUUID(), module.getId(), "Тема", null, position, TopicStatus.DRAFT
        ));
    }

    private StudentProgramEntity copyWithStatus(StudentProgramEntity source, StudentProgramStatus status) {
        return new StudentProgramEntity(
                source.getId(), source.getStudentId(), source.getLearningProgramId(), source.getAssignedByTeacherId(),
                status, source.getReportIntervalMinutes(), source.getStartedAt(), source.getCompletedAt(),
                source.getVersion(), source.getCreatedAt(), source.getUpdatedAt()
        );
    }

    private TopicEntity copyWithTitle(TopicEntity source, String title) {
        return new TopicEntity(
                source.getId(), source.getModuleId(), title, source.getDescription(), source.getPosition(),
                source.getStatus(), source.getVersion(), source.getCreatedAt(), source.getUpdatedAt()
        );
    }

    private <T> void assertOptimisticLocking(
            Class<T> type,
            UUID id,
            Consumer<T> firstChange,
            Consumer<T> staleChange
    ) {
        EntityManager firstEntityManager = entityManagerFactory.createEntityManager();
        EntityManager staleEntityManager = entityManagerFactory.createEntityManager();
        try {
            firstEntityManager.getTransaction().begin();
            staleEntityManager.getTransaction().begin();
            T first = firstEntityManager.find(type, id);
            T stale = staleEntityManager.find(type, id);

            firstChange.accept(first);
            firstEntityManager.getTransaction().commit();

            staleChange.accept(stale);
            Throwable thrown = catchThrowable(() -> staleEntityManager.getTransaction().commit());
            assertThat(thrown).as("stale %s update", type.getSimpleName()).isNotNull();
            assertThat(hasOptimisticLockCause(thrown))
                    .as("%s must fail specifically because of optimistic locking", type.getSimpleName())
                    .isTrue();
        } finally {
            if (firstEntityManager.getTransaction().isActive()) firstEntityManager.getTransaction().rollback();
            if (staleEntityManager.getTransaction().isActive()) staleEntityManager.getTransaction().rollback();
            firstEntityManager.close();
            staleEntityManager.close();
        }
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
