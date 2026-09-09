package com.tutorplatform.submission.infrastructure.persistence;

import com.tutorplatform.homework.domain.HomeworkEntity;
import com.tutorplatform.homework.domain.HomeworkItemEntity;
import com.tutorplatform.homework.domain.HomeworkRepository;
import com.tutorplatform.homework.domain.HomeworkStatus;
import com.tutorplatform.homework.infrastructure.persistence.JpaHomeworkRepository;
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
import com.tutorplatform.submission.domain.SubmissionAttemptContext;
import com.tutorplatform.submission.domain.SubmissionEntity;
import com.tutorplatform.submission.domain.SubmissionRepository;
import com.tutorplatform.submission.domain.SubmissionStatus;
import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskEntity;
import com.tutorplatform.task.domain.task.TaskRepository;
import com.tutorplatform.task.domain.task.TaskStatus;
import com.tutorplatform.task.domain.task.TaskType;
import com.tutorplatform.task.infrastructure.persistence.task.JpaTaskRepository;
import com.tutorplatform.user.domain.TeacherEntity;
import com.tutorplatform.user.domain.TeacherRepository;
import com.tutorplatform.user.domain.UserEntity;
import com.tutorplatform.user.domain.UserRepository;
import com.tutorplatform.user.domain.UserRole;
import com.tutorplatform.user.domain.UserStatus;
import com.tutorplatform.user.infrastructure.persistence.JpaTeacherRepository;
import com.tutorplatform.user.infrastructure.persistence.JpaUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        JpaHomeworkRepository.class,
        JpaSubmissionRepository.class
})
class SubmissionPersistenceIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.target", () -> "007");
    }

    @Autowired private UserRepository userRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private LearningProgramRepository learningProgramRepository;
    @Autowired private StudentProgramRepository studentProgramRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private HomeworkRepository homeworkRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void textSubmissionIsSavedWithAllFields() {
        Fixture fixture = createFixture(true);
        Instant submittedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);

        SubmissionEntity saved = submissionRepository.saveAndFlush(new SubmissionEntity(
                UUID.randomUUID(), fixture.studentId(), fixture.studentProgramId(), fixture.taskId(),
                fixture.homeworkItemId(), 1, SubmissionStatus.NEEDS_REVIEW, "Развёрнутый ответ", submittedAt
        ));

        assertThat(submissionRepository.findById(saved.getId())).get().satisfies(found -> {
            assertThat(found.getStudentId()).isEqualTo(fixture.studentId());
            assertThat(found.getStudentProgramId()).isEqualTo(fixture.studentProgramId());
            assertThat(found.getTaskId()).isEqualTo(fixture.taskId());
            assertThat(found.getHomeworkItemId()).isEqualTo(fixture.homeworkItemId());
            assertThat(found.getAttemptNo()).isEqualTo(1);
            assertThat(found.getStatus()).isEqualTo(SubmissionStatus.NEEDS_REVIEW);
            assertThat(found.getTextAnswer()).isEqualTo("Развёрнутый ответ");
            assertThat(found.getSubmittedAt()).isEqualTo(submittedAt);
            assertThat(found.getCreatedAt()).isNotNull();
        });
    }

    @Test
    void submissionWithoutHomeworkItemIsSaved() {
        Fixture fixture = createFixture(false);
        SubmissionEntity saved = submissionRepository.saveAndFlush(submission(fixture, null, 1));

        assertThat(submissionRepository.findById(saved.getId()))
                .get().extracting(SubmissionEntity::getHomeworkItemId).isNull();
    }

    @Test
    void submissionWithHomeworkItemIsSaved() {
        Fixture fixture = createFixture(true);
        SubmissionEntity saved = submissionRepository.saveAndFlush(
                submission(fixture, fixture.homeworkItemId(), 1)
        );

        assertThat(submissionRepository.findById(saved.getId()))
                .get().extracting(SubmissionEntity::getHomeworkItemId)
                .isEqualTo(fixture.homeworkItemId());
    }

    @Test
    void nonPositiveAttemptNumberIsRejectedByDatabaseConstraint() {
        Fixture fixture = createFixture(false);

        assertThatThrownBy(() -> insertSubmission(
                fixture, UUID.randomUUID(), fixture.studentId(), fixture.studentProgramId(),
                fixture.taskId(), null, 0, "SUBMITTED", null
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void everySubmissionStatusIsPersisted() {
        Fixture fixture = createFixture(false);
        int attemptNo = 1;
        for (SubmissionStatus status : SubmissionStatus.values()) {
            SubmissionEntity saved = submissionRepository.saveAndFlush(new SubmissionEntity(
                    UUID.randomUUID(), fixture.studentId(), fixture.studentProgramId(), fixture.taskId(),
                    null, attemptNo++, status, "Ответ", Instant.now()
            ));
            assertThat(submissionRepository.findById(saved.getId()))
                    .get().extracting(SubmissionEntity::getStatus).isEqualTo(status);
        }
    }

    @Test
    void studentForeignKeyIsEnforced() {
        assertInvalidForeignKey(ForeignKey.STUDENT);
    }

    @Test
    void studentProgramForeignKeyIsEnforced() {
        assertInvalidForeignKey(ForeignKey.STUDENT_PROGRAM);
    }

    @Test
    void taskForeignKeyIsEnforced() {
        assertInvalidForeignKey(ForeignKey.TASK);
    }

    @Test
    void homeworkItemForeignKeyIsEnforced() {
        assertInvalidForeignKey(ForeignKey.HOMEWORK_ITEM);
    }

    @Test
    void textAnswerIsNullableAtDatabaseLevel() {
        Fixture fixture = createFixture(false);
        SubmissionEntity saved = submissionRepository.saveAndFlush(new SubmissionEntity(
                UUID.randomUUID(), fixture.studentId(), fixture.studentProgramId(), fixture.taskId(),
                null, 1, SubmissionStatus.SUBMITTED, null, Instant.now()
        ));

        assertThat(submissionRepository.findById(saved.getId()))
                .get().extracting(SubmissionEntity::getTextAnswer).isNull();
    }

    @Test
    void attemptsAreFilteredByExactContextAndOrderedNewestFirst() {
        Fixture fixture = createFixture(true);
        submissionRepository.saveAndFlush(submission(fixture, fixture.homeworkItemId(), 1));
        submissionRepository.saveAndFlush(submission(fixture, fixture.homeworkItemId(), 3));
        submissionRepository.saveAndFlush(submission(fixture, fixture.homeworkItemId(), 2));
        submissionRepository.saveAndFlush(submission(fixture, null, 1));

        SubmissionAttemptContext context = context(fixture, fixture.homeworkItemId());

        assertThat(submissionRepository.findAttempts(context, 0, 10).items())
                .extracting(SubmissionEntity::getAttemptNo)
                .containsExactly(3, 2, 1);
        assertThat(submissionRepository.findLatestAttempt(context))
                .get().extracting(SubmissionEntity::getAttemptNo).isEqualTo(3);
        assertThat(submissionRepository.existsByStatus(context, SubmissionStatus.SUBMITTED)).isTrue();
    }

    @Test
    void studentLookupIsOwnershipSafe() {
        Fixture owner = createFixture(false);
        Fixture other = createFixture(false);
        SubmissionEntity saved = submissionRepository.saveAndFlush(submission(owner, null, 1));

        assertThat(submissionRepository.findByIdAndStudentId(saved.getId(), owner.studentId())).isPresent();
        assertThat(submissionRepository.findByIdAndStudentId(saved.getId(), other.studentId())).isEmpty();
    }

    @Test
    void studentListUsesDatabasePagination() {
        Fixture fixture = createFixture(false);
        submissionRepository.saveAndFlush(submission(fixture, null, 1));
        submissionRepository.saveAndFlush(submission(fixture, null, 2));
        submissionRepository.saveAndFlush(submission(fixture, null, 3));
        Fixture other = createFixture(false);
        submissionRepository.saveAndFlush(submission(other, null, 1));

        var firstPage = submissionRepository.findPageByStudentId(fixture.studentId(), 0, 2);
        var secondPage = submissionRepository.findPageByStudentId(fixture.studentId(), 1, 2);

        assertThat(firstPage.items()).hasSize(2);
        assertThat(secondPage.items()).hasSize(1);
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.items()).allMatch(item -> item.getStudentId().equals(fixture.studentId()));
    }

    @Test
    void nextAttemptNumberUsesPersistedExactContext() {
        Fixture fixture = createFixture(true);
        SubmissionAttemptContext homeworkContext = context(fixture, fixture.homeworkItemId());
        SubmissionAttemptContext practiceContext = context(fixture, null);
        submissionRepository.saveAndFlush(submission(fixture, fixture.homeworkItemId(), 1));
        submissionRepository.saveAndFlush(submission(fixture, fixture.homeworkItemId(), 2));
        submissionRepository.saveAndFlush(submission(fixture, null, 1));

        assertThat(submissionRepository.nextAttemptNo(homeworkContext)).isEqualTo(3);
        assertThat(submissionRepository.nextAttemptNo(practiceContext)).isEqualTo(2);
    }

    @Test
    void duplicateAttemptInV007ContextIsRejected() {
        Fixture fixture = createFixture(false);
        submissionRepository.saveAndFlush(submission(fixture, null, 1));

        assertThatThrownBy(() -> submissionRepository.saveAndFlush(submission(fixture, null, 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void codeSubmissionJavaBehaviorIsNotIntroduced() {
        assertThatThrownBy(() -> Class.forName("com.tutorplatform.submission.domain.CodeSubmissionEntity"))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName("com.tutorplatform.submission.application.CodeExecutionService"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    private void assertInvalidForeignKey(ForeignKey foreignKey) {
        Fixture fixture = createFixture(true);
        assertThatThrownBy(() -> insertSubmission(
                fixture,
                UUID.randomUUID(),
                foreignKey == ForeignKey.STUDENT ? UUID.randomUUID() : fixture.studentId(),
                foreignKey == ForeignKey.STUDENT_PROGRAM ? UUID.randomUUID() : fixture.studentProgramId(),
                foreignKey == ForeignKey.TASK ? UUID.randomUUID() : fixture.taskId(),
                foreignKey == ForeignKey.HOMEWORK_ITEM ? UUID.randomUUID() : fixture.homeworkItemId(),
                1,
                "SUBMITTED",
                "Ответ"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertSubmission(
            Fixture fixture,
            UUID id,
            UUID studentId,
            UUID studentProgramId,
            UUID taskId,
            UUID homeworkItemId,
            int attemptNo,
            String status,
            String textAnswer
    ) {
        jdbcTemplate.update(
                """
                    insert into submissions(
                        id, student_id, student_program_id, task_id, homework_item_id,
                        attempt_no, status, text_answer
                    ) values (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                id, studentId, studentProgramId, taskId, homeworkItemId, attemptNo, status, textAnswer
        );
    }

    private SubmissionEntity submission(Fixture fixture, UUID homeworkItemId, int attemptNo) {
        return new SubmissionEntity(
                UUID.randomUUID(), fixture.studentId(), fixture.studentProgramId(), fixture.taskId(),
                homeworkItemId, attemptNo, SubmissionStatus.SUBMITTED, "Ответ", Instant.now()
        );
    }

    private SubmissionAttemptContext context(Fixture fixture, UUID homeworkItemId) {
        return new SubmissionAttemptContext(
                fixture.studentId(), fixture.studentProgramId(), fixture.taskId(), homeworkItemId
        );
    }

    private Fixture createFixture(boolean withHomework) {
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
        TaskEntity task = taskRepository.saveAndFlush(new TaskEntity(
                UUID.randomUUID(), teacher.id(), subject.id(), "Текстовое задание", "Условие",
                TaskType.TEXT, TaskDifficulty.EASY, TaskStatus.ACTIVE
        ));
        UUID homeworkItemId = null;
        if (withHomework) {
            UUID homeworkId = UUID.randomUUID();
            homeworkItemId = UUID.randomUUID();
            HomeworkEntity homework = homeworkRepository.saveAndFlush(new HomeworkEntity(
                    homeworkId, studentProgram.id(), teacher.id(), "Домашняя работа", null,
                    Instant.now(), null, HomeworkStatus.ASSIGNED, null,
                    List.of(new HomeworkItemEntity(homeworkItemId, homeworkId, task.getId(), 0, true))
            ));
            homeworkItemId = homework.getItems().getFirst().id();
        }
        return new Fixture(student.getId(), studentProgram.id(), task.getId(), homeworkItemId);
    }

    private enum ForeignKey {
        STUDENT,
        STUDENT_PROGRAM,
        TASK,
        HOMEWORK_ITEM
    }

    private record Fixture(
            UUID studentId,
            UUID studentProgramId,
            UUID taskId,
            UUID homeworkItemId
    ) {
    }
}
