package com.tutorplatform.homework.infrastructure.persistence;

import com.tutorplatform.homework.domain.HomeworkEntity;
import com.tutorplatform.homework.domain.HomeworkItemEntity;
import com.tutorplatform.homework.domain.HomeworkRepository;
import com.tutorplatform.homework.domain.HomeworkStatus;
import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import com.tutorplatform.program.domain.studentprogram.StudentProgramEntity;
import com.tutorplatform.program.domain.studentprogram.StudentProgramRepository;
import com.tutorplatform.program.domain.studentprogram.StudentProgramStatus;
import com.tutorplatform.program.infrastructure.persistence.learningprogram.JpaLearningProgramRepository;
import com.tutorplatform.program.infrastructure.persistence.studentprogram.JpaStudentProgramRepository;
import com.tutorplatform.student.domain.StudentEntity;
import com.tutorplatform.student.domain.StudentRepository;
import com.tutorplatform.student.domain.StudentStatus;
import com.tutorplatform.student.infrastructure.persistence.JpaStudentRepository;
import com.tutorplatform.subject.domain.SubjectEntity;
import com.tutorplatform.subject.domain.SubjectRepository;
import com.tutorplatform.subject.domain.SubjectStatus;
import com.tutorplatform.subject.infrastructure.persistence.JpaSubjectRepository;
import com.tutorplatform.task.domain.task.*;
import com.tutorplatform.task.infrastructure.persistence.task.JpaTaskRepository;
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
import java.util.List;
import java.util.UUID;

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
    JpaTaskRepository.class,
    JpaHomeworkRepository.class
})
class HomeworkPersistenceIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private LearningProgramRepository learningProgramRepository;
    @Autowired
    private StudentProgramRepository studentProgramRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private HomeworkRepository homeworkRepository;
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
        registry.add("spring.flyway.target", () -> "007");
    }

    @Test
    void flywayMigrationV007AppliesSuccessfully() {
        assertThat(Arrays.stream(flyway.info().applied())
            .map(migration -> migration.getVersion().toString()))
            .containsExactly("001", "002", "003", "004", "005", "006", "007");

        assertThat(jdbcTemplate.queryForList(
            """
                select table_name
                from information_schema.tables
                where table_schema = 'public'
                  and table_name in ('homeworks', 'homework_items', 'submissions', 'code_submissions')
                order by table_name
                """,
            String.class
        )).containsExactly("code_submissions", "homework_items", "homeworks", "submissions");
    }

    @Test
    void homeworkIsSavedTogetherWithItems() {
        HomeworkFixture fixture = createFixture();
        HomeworkEntity homework = saveHomework(fixture, HomeworkStatus.ASSIGNED, null, twoItems(fixture));

        HomeworkEntity persisted = homeworkRepository.findByIdWithItems(homework.getId()).orElseThrow();

        assertThat(persisted.getStudentProgramId()).isEqualTo(fixture.studentProgram().id());
        assertThat(persisted.getAssignedByTeacherId()).isEqualTo(fixture.teacher().id());
        assertThat(persisted.getTitle()).isEqualTo("Домашняя работа");
        assertThat(persisted.getDescription()).isEqualTo("Описание");
        assertThat(persisted.getItems()).extracting(HomeworkItemEntity::taskId)
            .containsExactly(fixture.firstTask().getId(), fixture.secondTask().getId());
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getUpdatedAt()).isNotNull();
        assertThat(persisted.getVersion()).isZero();
    }

    @Test
    void homeworkStudentProgramForeignKeyWorks() {
        HomeworkFixture fixture = createFixture();

        assertThatThrownBy(() -> insertHomework(UUID.randomUUID(), fixture.teacher().id()))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void homeworkTeacherForeignKeyWorks() {
        HomeworkFixture fixture = createFixture();

        assertThatThrownBy(() -> insertHomework(fixture.studentProgram().id(), UUID.randomUUID()))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void assignedHomeworkStatusIsSaved() {
        assertStatusIsSaved(HomeworkStatus.ASSIGNED);
    }

    @Test
    void completedHomeworkStatusIsSaved() {
        assertStatusIsSaved(HomeworkStatus.COMPLETED);
    }

    @Test
    void cancelledHomeworkStatusIsSaved() {
        assertStatusIsSaved(HomeworkStatus.CANCELLED);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void homeworkOptimisticLockingWorks() {
        HomeworkFixture fixture = createFixture();
        HomeworkEntity saved = saveHomework(fixture, HomeworkStatus.ASSIGNED, null, List.of());
        TransactionTemplate transaction = requiresNewTransaction();

        HomeworkEntity first = transaction.execute(
            status -> homeworkRepository.findById(saved.getId()).orElseThrow()
        );
        HomeworkEntity stale = transaction.execute(
            status -> homeworkRepository.findById(saved.getId()).orElseThrow()
        );

        first.update("Первая версия", first.getDescription(), first.getDueAt(), first.getStatus(), null);
        transaction.executeWithoutResult(status -> homeworkRepository.saveAndFlush(first));

        stale.update("Устаревшая версия", stale.getDescription(), stale.getDueAt(), stale.getStatus(), null);
        Throwable thrown = catchThrowable(() -> transaction.executeWithoutResult(
            status -> homeworkRepository.saveAndFlush(stale)
        ));

        assertThat(thrown).isNotNull();
        assertThat(hasOptimisticLockCause(thrown)).isTrue();
        assertThat(homeworkRepository.findById(saved.getId()))
            .get().extracting(HomeworkEntity::getTitle)
            .isEqualTo("Первая версия");
    }

    @Test
    void updatingHomeworkPreservesItsItems() {
        HomeworkFixture fixture = createFixture();
        HomeworkEntity saved = saveHomework(fixture, HomeworkStatus.ASSIGNED, null, twoItems(fixture));
        HomeworkEntity aggregate = homeworkRepository.findById(saved.getId()).orElseThrow();

        aggregate.update("Обновлённая работа", aggregate.getDescription(), aggregate.getDueAt(),
            HomeworkStatus.COMPLETED, Instant.now());
        homeworkRepository.saveAndFlush(aggregate);

        HomeworkEntity updated = homeworkRepository.findByIdWithItems(saved.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("Обновлённая работа");
        assertThat(updated.getStatus()).isEqualTo(HomeworkStatus.COMPLETED);
        assertThat(updated.getItems()).hasSize(2);
    }

    @Test
    void homeworkItemIsSavedAndCanBeFoundInHomeworkContext() {
        HomeworkFixture fixture = createFixture();
        HomeworkItemEntity item = item(UUID.randomUUID(), fixture.homeworkId(), fixture.firstTask().getId(), 0);
        HomeworkEntity homework = saveHomework(fixture, HomeworkStatus.ASSIGNED, null, List.of(item));

        assertThat(homeworkRepository.findItemById(homework.getId(), item.id()))
            .get().extracting(HomeworkItemEntity::taskId)
            .isEqualTo(fixture.firstTask().getId());
        assertThat(homeworkRepository.findItemById(UUID.randomUUID(), item.id())).isEmpty();
    }

    @Test
    void multipleHomeworkItemsAreSavedForOneHomework() {
        HomeworkFixture fixture = createFixture();
        HomeworkEntity homework = saveHomework(fixture, HomeworkStatus.ASSIGNED, null, twoItems(fixture));

        assertThat(homeworkRepository.findByIdWithItems(homework.getId()).orElseThrow().getItems())
            .extracting(HomeworkItemEntity::position)
            .containsExactly(0, 1);
    }

    @Test
    void homeworkItemTaskForeignKeyWorks() {
        HomeworkFixture fixture = createFixture();
        HomeworkEntity homework = saveHomework(fixture, HomeworkStatus.ASSIGNED, null, List.of());

        assertThatThrownBy(() -> jdbcTemplate.update(
            "insert into homework_items(id, homework_id, task_id, position) values (?, ?, ?, 0)",
            UUID.randomUUID(), homework.getId(), UUID.randomUUID()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void negativeHomeworkItemPositionIsRejectedByDatabase() {
        HomeworkFixture fixture = createFixture();
        HomeworkEntity homework = saveHomework(fixture, HomeworkStatus.ASSIGNED, null, List.of());

        assertThatThrownBy(() -> jdbcTemplate.update(
            "insert into homework_items(id, homework_id, task_id, position) values (?, ?, ?, -1)",
            UUID.randomUUID(), homework.getId(), fixture.firstTask().getId()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicatePositionWithinHomeworkIsRejected() {
        HomeworkFixture fixture = createFixture();
        List<HomeworkItemEntity> items = List.of(
            item(UUID.randomUUID(), fixture.homeworkId(), fixture.firstTask().getId(), 0),
            item(UUID.randomUUID(), fixture.homeworkId(), fixture.secondTask().getId(), 0)
        );

        assertThatThrownBy(() -> saveHomework(fixture, HomeworkStatus.ASSIGNED, null, items))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingHomeworkCascadesHomeworkItems() {
        HomeworkFixture fixture = createFixture();
        HomeworkEntity homework = saveHomework(fixture, HomeworkStatus.ASSIGNED, null, twoItems(fixture));

        jdbcTemplate.update("delete from homeworks where id = ?", homework.getId());

        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from homework_items where homework_id = ?", Integer.class, homework.getId()
        )).isZero();
    }

    @Test
    void deletingHomeworkItemDoesNotDeleteTask() {
        HomeworkFixture fixture = createFixture();
        HomeworkEntity homework = saveHomework(fixture, HomeworkStatus.ASSIGNED, null, twoItems(fixture));
        UUID itemId = homework.getItems().getFirst().id();

        jdbcTemplate.update("delete from homework_items where id = ?", itemId);

        assertThat(taskRepository.findById(fixture.firstTask().getId())).isPresent();
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from homework_items where id = ?", Integer.class, itemId
        )).isZero();
    }

    @Test
    void overdueIsDerivedAndCannotBeStoredAsHomeworkStatus() {
        HomeworkFixture fixture = createFixture();

        assertThat(Arrays.stream(HomeworkStatus.values()).map(Enum::name))
            .doesNotContain("OVERDUE");
        assertThatThrownBy(() -> jdbcTemplate.update(
            """
                insert into homeworks(id, student_program_id, assigned_by_teacher_id, title, status)
                values (?, ?, ?, 'Просрочено', 'OVERDUE')
                """,
            fixture.homeworkId(), fixture.studentProgram().id(), fixture.teacher().id()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void homeworkListIsFilteredByStudentProgramAndUsesDatabasePagination() {
        HomeworkFixture fixture = createFixture();
        saveHomework(fixture, HomeworkStatus.ASSIGNED, null, List.of());
        HomeworkFixture other = createFixture();
        saveHomework(other, HomeworkStatus.ASSIGNED, null, List.of());

        assertThat(homeworkRepository.findAllByStudentProgramId(fixture.studentProgram().id(), 0, 1))
            .extracting(HomeworkEntity::getStudentProgramId)
            .containsExactly(fixture.studentProgram().id());
        assertThat(homeworkRepository.existsByIdAndStudentProgramId(
            fixture.homeworkId(), fixture.studentProgram().id()
        )).isTrue();
        assertThat(homeworkRepository.existsByIdAndStudentProgramId(
            fixture.homeworkId(), other.studentProgram().id()
        )).isFalse();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void submissionsSchemaMatchesErModelConstraints() {
        HomeworkFixture fixture = createFixture();
        HomeworkEntity homework = saveHomework(fixture, HomeworkStatus.ASSIGNED, null, twoItems(fixture));
        UUID homeworkItemId = homework.getItems().getFirst().id();
        UUID submissionId = insertSubmission(fixture, homeworkItemId, 1, "SUBMITTED");

        assertThat(jdbcTemplate.queryForObject(
            "select text_answer from submissions where id = ?", String.class, submissionId
        )).isEqualTo("Ответ");
        assertThatThrownBy(() -> insertSubmission(fixture, homeworkItemId, 1, "PASSED"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertSubmission(fixture, homeworkItemId, 0, "SUBMITTED"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertSubmission(fixture, homeworkItemId, 2, "PENDING"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void codeSubmissionsSchemaMatchesErModelConstraints() {
        HomeworkFixture fixture = createFixture();
        UUID submissionId = insertSubmission(fixture, null, 1, "SUBMITTED");

        jdbcTemplate.update(
            """
                insert into code_submissions(
                    submission_id, source_code, execution_status, passed_tests, total_tests, execution_time_ms
                ) values (?, 'print(1)', 'PASSED', 1, 1, 12)
                """,
            submissionId
        );

        assertThat(jdbcTemplate.queryForObject(
            "select source_code from code_submissions where submission_id = ?", String.class, submissionId
        )).isEqualTo("print(1)");
        assertThatThrownBy(() -> jdbcTemplate.update(
            """
                insert into code_submissions(
                    submission_id, source_code, execution_status, passed_tests, total_tests
                ) values (?, 'x', 'UNKNOWN', 0, 0)
                """,
            UUID.randomUUID()
        )).isInstanceOf(DataIntegrityViolationException.class);

        UUID invalidCountsSubmission = insertSubmission(fixture, null, 2, "SUBMITTED");
        assertThatThrownBy(() -> jdbcTemplate.update(
            """
                insert into code_submissions(
                    submission_id, source_code, execution_status, passed_tests, total_tests
                ) values (?, 'x', 'FAILED', 2, 1)
                """,
            invalidCountsSubmission
        )).isInstanceOf(DataIntegrityViolationException.class);

        jdbcTemplate.update("delete from submissions where id = ?", submissionId);
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from code_submissions where submission_id = ?", Integer.class, submissionId
        )).isZero();
    }

    private HomeworkEntity saveHomework(
        HomeworkFixture fixture,
        HomeworkStatus status,
        Instant completedAt,
        List<HomeworkItemEntity> items
    ) {
        return homeworkRepository.saveAndFlush(new HomeworkEntity(
            fixture.homeworkId(), fixture.studentProgram().id(), fixture.teacher().id(),
            "Домашняя работа", "Описание", Instant.now(), Instant.now().plusSeconds(3600),
            status, completedAt, items
        ));
    }

    private void assertStatusIsSaved(HomeworkStatus status) {
        HomeworkFixture fixture = createFixture();
        Instant completedAt = status == HomeworkStatus.COMPLETED ? Instant.now() : null;
        HomeworkEntity homework = saveHomework(fixture, status, completedAt, List.of());

        assertThat(homeworkRepository.findById(homework.getId()))
            .get().extracting(HomeworkEntity::getStatus)
            .isEqualTo(status);
    }

    private List<HomeworkItemEntity> twoItems(HomeworkFixture fixture) {
        return List.of(
            item(UUID.randomUUID(), fixture.homeworkId(), fixture.firstTask().getId(), 0),
            item(UUID.randomUUID(), fixture.homeworkId(), fixture.secondTask().getId(), 1)
        );
    }

    private HomeworkItemEntity item(UUID id, UUID homeworkId, UUID taskId, int position) {
        return new HomeworkItemEntity(id, homeworkId, taskId, position, true);
    }

    private int insertHomework(UUID studentProgramId, UUID teacherId) {
        return jdbcTemplate.update(
            """
                insert into homeworks(id, student_program_id, assigned_by_teacher_id, title)
                values (?, ?, ?, 'Домашняя работа')
                """,
            UUID.randomUUID(), studentProgramId, teacherId
        );
    }

    private UUID insertSubmission(
        HomeworkFixture fixture,
        UUID homeworkItemId,
        int attemptNo,
        String status
    ) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
            """
                insert into submissions(
                    id, student_id, student_program_id, task_id, homework_item_id,
                    attempt_no, status, text_answer
                ) values (?, ?, ?, ?, ?, ?, ?, 'Ответ')
                """,
            id, fixture.student().getId(), fixture.studentProgram().id(),
            fixture.firstTask().getId(), homeworkItemId, attemptNo, status
        );
        return id;
    }

    private HomeworkFixture createFixture() {
        UserEntity user = new UserEntity(
            UUID.randomUUID(), UUID.randomUUID() + "@example.com", "password-hash", UserStatus.ACTIVE
        );
        user.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(user);
        TeacherEntity teacher = teacherRepository.saveAndFlush(
            new TeacherEntity(UUID.randomUUID(), user, "Teacher")
        );
        StudentEntity student = studentRepository.saveAndFlush(new StudentEntity(
            UUID.randomUUID(), "Ученик", null, StudentStatus.ACTIVE
        ));
        SubjectEntity subject = subjectRepository.saveAndFlush(new SubjectEntity(
            UUID.randomUUID(), teacher.id(), null, "Предмет " + UUID.randomUUID(), null,
            SubjectStatus.ACTIVE
        ));
        LearningProgramEntity program = learningProgramRepository.saveAndFlush(new LearningProgramEntity(
            UUID.randomUUID(), teacher.id(), subject.id(), "Программа", null,
            LearningProgramStatus.ACTIVE
        ));
        StudentProgramEntity studentProgram = studentProgramRepository.saveAndFlush(new StudentProgramEntity(
            UUID.randomUUID(), student.getId(), program.getId(), teacher.id(),
            StudentProgramStatus.ACTIVE, 480, Instant.now(), null
        ));
        TaskEntity firstTask = createTask(teacher, subject, "Первое задание");
        TaskEntity secondTask = createTask(teacher, subject, "Второе задание");
        return new HomeworkFixture(
            UUID.randomUUID(), teacher, student, studentProgram, firstTask, secondTask
        );
    }

    private TaskEntity createTask(TeacherEntity teacher, SubjectEntity subject, String title) {
        return taskRepository.saveAndFlush(new TaskEntity(
            UUID.randomUUID(), teacher.id(), subject.id(), title, "Условие",
            TaskType.TEXT, TaskDifficulty.EASY, TaskStatus.ACTIVE
        ));
    }

    private TransactionTemplate requiresNewTransaction() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction;
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

    private record HomeworkFixture(
        UUID homeworkId,
        TeacherEntity teacher,
        StudentEntity student,
        StudentProgramEntity studentProgram,
        TaskEntity firstTask,
        TaskEntity secondTask
    ) {
    }
}
