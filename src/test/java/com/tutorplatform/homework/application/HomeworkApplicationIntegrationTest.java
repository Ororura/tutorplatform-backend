package com.tutorplatform.homework.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.homework.application.exception.HomeworkItemPositionConflictException;
import com.tutorplatform.homework.application.exception.HomeworkNotFoundException;
import com.tutorplatform.homework.application.exception.HomeworkStudentProgramNotFoundException;
import com.tutorplatform.homework.application.exception.HomeworkTaskNotAssignableException;
import com.tutorplatform.homework.application.exception.HomeworkTaskSubjectMismatchException;
import com.tutorplatform.homework.application.exception.HomeworkVersionConflictException;
import com.tutorplatform.homework.application.exception.InvalidHomeworkException;
import com.tutorplatform.homework.domain.HomeworkRepository;
import com.tutorplatform.homework.domain.HomeworkStatus;
import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import com.tutorplatform.program.domain.studentprogram.StudentProgramEntity;
import com.tutorplatform.program.domain.studentprogram.StudentProgramRepository;
import com.tutorplatform.program.domain.studentprogram.StudentProgramStatus;
import com.tutorplatform.student.application.exception.StudentNotFoundException;
import com.tutorplatform.student.domain.StudentEntity;
import com.tutorplatform.student.domain.StudentRepository;
import com.tutorplatform.student.domain.StudentStatus;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkEntity;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkRepository;
import com.tutorplatform.subject.domain.SubjectEntity;
import com.tutorplatform.subject.domain.SubjectRepository;
import com.tutorplatform.subject.domain.SubjectStatus;
import com.tutorplatform.task.application.exception.TaskNotFoundException;
import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskEntity;
import com.tutorplatform.task.domain.task.TaskRepository;
import com.tutorplatform.task.domain.task.TaskStatus;
import com.tutorplatform.task.domain.task.TaskType;
import com.tutorplatform.user.domain.TeacherEntity;
import com.tutorplatform.user.domain.TeacherRepository;
import com.tutorplatform.user.domain.UserEntity;
import com.tutorplatform.user.domain.UserRepository;
import com.tutorplatform.user.domain.UserRole;
import com.tutorplatform.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class HomeworkApplicationIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.target", () -> "007");
    }

    @Autowired private HomeworkService homeworkService;
    @Autowired private HomeworkRepository homeworkRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherStudentLinkRepository teacherStudentLinkRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private LearningProgramRepository learningProgramRepository;
    @Autowired private StudentProgramRepository studentProgramRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void teacherCreatesHomeworkWithServerAssignmentContextAndMultipleItems() {
        Fixture fixture = createFixture("create-homework@example.com");

        HomeworkResult created = createHomework(fixture, "  Домашняя работа  ", null, fixture.tasks());

        assertThat(created.title()).isEqualTo("Домашняя работа");
        assertThat(created.studentProgramId()).isEqualTo(fixture.studentProgram().id());
        assertThat(created.status()).isEqualTo(HomeworkStatus.ASSIGNED);
        assertThat(created.items()).extracting(HomeworkItemResult::taskId)
                .containsExactly(fixture.firstTask().getId(), fixture.secondTask().getId());
        assertThat(homeworkRepository.findById(created.id()).orElseThrow().getAssignedByTeacherId())
                .isEqualTo(fixture.teacher().id());
        assertThat(created.assignedAt()).isNotNull();
    }

    @Test
    void taskOwnedByAnotherTeacherIsRejected() {
        Fixture fixture = createFixture("task-owner@example.com");
        Fixture foreign = createFixture("task-foreign@example.com");

        assertThatThrownBy(() -> createHomework(
                fixture, "ДЗ", null, List.of(foreign.firstTask())
        )).isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void studentOwnedByAnotherTeacherIsNotDisclosed() {
        Fixture owner = createFixture("student-owner@example.com");
        Fixture foreign = createFixture("student-foreign@example.com");

        assertThatThrownBy(() -> homeworkService.createHomework(
                foreign.principal(), createCommand(owner, "ДЗ", null, owner.tasks())
        )).isInstanceOf(StudentNotFoundException.class);
    }

    @Test
    void studentProgramOfAnotherStudentIsRejected() {
        TeacherFixture teacher = createTeacher("program-student@example.com");
        Fixture first = createFixture(teacher, "Первый");
        Fixture second = createFixture(teacher, "Второй");
        CreateHomeworkCommand command = new CreateHomeworkCommand(
                first.student().getId(), second.studentProgram().id(), "ДЗ", null, null,
                itemInputs(first.tasks())
        );

        assertThatThrownBy(() -> homeworkService.createHomework(teacher.principal(), command))
                .isInstanceOf(HomeworkStudentProgramNotFoundException.class);
    }

    @Test
    void taskFromAnotherSubjectIsRejected() {
        TeacherFixture teacher = createTeacher("cross-subject@example.com");
        Fixture first = createFixture(teacher, "Первый");
        Fixture otherSubject = createFixture(teacher, "Второй");

        assertThatThrownBy(() -> createHomework(
                first, "ДЗ", null, List.of(otherSubject.firstTask())
        )).isInstanceOf(HomeworkTaskSubjectMismatchException.class);
    }

    @Test
    void codeTaskIsRejectedInTextSlice() {
        Fixture fixture = createFixture("code-task@example.com");
        TaskEntity codeTask = createTask(
                fixture.teacher(), fixture.subject(), "CODE", TaskType.CODE, TaskStatus.ACTIVE
        );

        assertThatThrownBy(() -> createHomework(fixture, "ДЗ", null, List.of(codeTask)))
                .isInstanceOf(HomeworkTaskNotAssignableException.class);
    }

    @Test
    void inactiveTaskIsRejected() {
        Fixture fixture = createFixture("inactive-task@example.com");
        TaskEntity draft = createTask(
                fixture.teacher(), fixture.subject(), "Черновик", TaskType.TEXT, TaskStatus.DRAFT
        );

        assertThatThrownBy(() -> createHomework(fixture, "ДЗ", null, List.of(draft)))
                .isInstanceOf(HomeworkTaskNotAssignableException.class);
    }

    @Test
    void duplicateTaskIsRejected() {
        Fixture fixture = createFixture("duplicate-task@example.com");

        assertThatThrownBy(() -> homeworkService.createHomework(
                fixture.principal(), new CreateHomeworkCommand(
                        fixture.student().getId(), fixture.studentProgram().id(), "ДЗ", null, null,
                        List.of(
                                new HomeworkItemInput(fixture.firstTask().getId(), 0, true),
                                new HomeworkItemInput(fixture.firstTask().getId(), 1, true)
                        )
                )
        )).isInstanceOf(InvalidHomeworkException.class);
    }

    @Test
    void duplicatePositionIsRejected() {
        Fixture fixture = createFixture("duplicate-position@example.com");

        assertThatThrownBy(() -> homeworkService.createHomework(
                fixture.principal(), new CreateHomeworkCommand(
                        fixture.student().getId(), fixture.studentProgram().id(), "ДЗ", null, null,
                        List.of(
                                new HomeworkItemInput(fixture.firstTask().getId(), 0, true),
                                new HomeworkItemInput(fixture.secondTask().getId(), 0, true)
                        )
                )
        )).isInstanceOf(HomeworkItemPositionConflictException.class);
    }

    @Test
    void negativePositionIsRejected() {
        Fixture fixture = createFixture("negative-position@example.com");

        assertThatThrownBy(() -> homeworkService.createHomework(
                fixture.principal(), new CreateHomeworkCommand(
                        fixture.student().getId(), fixture.studentProgram().id(), "ДЗ", null, null,
                        List.of(new HomeworkItemInput(fixture.firstTask().getId(), -1, true))
                )
        )).isInstanceOf(InvalidHomeworkException.class);
    }

    @Test
    void invalidTaskLeavesNoPartialHomework() {
        Fixture fixture = createFixture("rollback-homework@example.com");
        long before = homeworkCount();

        assertThatThrownBy(() -> homeworkService.createHomework(
                fixture.principal(), new CreateHomeworkCommand(
                        fixture.student().getId(), fixture.studentProgram().id(), "ДЗ", null, null,
                        List.of(
                                new HomeworkItemInput(fixture.firstTask().getId(), 0, true),
                                new HomeworkItemInput(UUID.randomUUID(), 1, true)
                        )
                )
        )).isInstanceOf(TaskNotFoundException.class);

        assertThat(homeworkCount()).isEqualTo(before);
    }

    @Test
    void teacherReadsOwnedHomeworkWithItemsOrderedByPosition() {
        Fixture fixture = createFixture("read-homework@example.com");
        HomeworkResult created = homeworkService.createHomework(
                fixture.principal(), new CreateHomeworkCommand(
                        fixture.student().getId(), fixture.studentProgram().id(), "ДЗ", "Описание", null,
                        List.of(
                                new HomeworkItemInput(fixture.secondTask().getId(), 1, false),
                                new HomeworkItemInput(fixture.firstTask().getId(), 0, true)
                        )
                )
        );

        HomeworkResult found = homeworkService.getHomework(
                fixture.principal(), fixture.student().getId(), created.id()
        );

        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.items()).extracting(HomeworkItemResult::position).containsExactly(0, 1);
        assertThat(found.items()).extracting(HomeworkItemResult::taskTitle)
                .containsExactly("Первая задача", "Вторая задача");
    }

    @Test
    void homeworkOfAnotherStudentIsNotDisclosed() {
        TeacherFixture teacher = createTeacher("hidden-homework@example.com");
        Fixture first = createFixture(teacher, "Первый");
        Fixture second = createFixture(teacher, "Второй");
        HomeworkResult homework = createHomework(first, "ДЗ", null, first.tasks());

        assertThatThrownBy(() -> homeworkService.getHomework(
                teacher.principal(), second.student().getId(), homework.id()
        )).isInstanceOf(HomeworkNotFoundException.class);
    }

    @Test
    void listReturnsOnlySpecifiedStudentAndSupportsPagination() {
        TeacherFixture teacher = createTeacher("list-homework@example.com");
        Fixture first = createFixture(teacher, "Первый");
        Fixture second = createFixture(teacher, "Второй");
        createHomework(first, "A", null, first.tasks());
        createHomework(first, "B", null, first.tasks());
        createHomework(first, "C", null, first.tasks());
        createHomework(second, "Foreign", null, second.tasks());

        HomeworkPageResult page0 = list(first, null, 0, 2, "title,asc");
        HomeworkPageResult page1 = list(first, null, 1, 2, "title,asc");

        assertThat(page0.items()).extracting(HomeworkSummaryResult::title).containsExactly("A", "B");
        assertThat(page1.items()).extracting(HomeworkSummaryResult::title).containsExactly("C");
        assertThat(page0.totalElements()).isEqualTo(3);
        assertThat(page0.totalPages()).isEqualTo(2);
    }

    @Test
    void listFiltersByStatusAndStudentProgram() {
        Fixture fixture = createFixture("filter-homework@example.com");
        HomeworkResult assigned = createHomework(fixture, "Assigned", null, fixture.tasks());
        HomeworkResult cancelled = createHomework(fixture, "Cancelled", null, fixture.tasks());
        homeworkService.cancelHomework(fixture.principal(), fixture.student().getId(), cancelled.id());

        HomeworkPageResult result = list(fixture, HomeworkStatus.CANCELLED, 0, 20, "assignedAt,desc");

        assertThat(result.items()).extracting(HomeworkSummaryResult::id).containsExactly(cancelled.id());
        assertThat(result.items()).extracting(HomeworkSummaryResult::id).doesNotContain(assigned.id());
        assertThat(result.items()).allSatisfy(item ->
                assertThat(item.studentProgramId()).isEqualTo(fixture.studentProgram().id())
        );
    }

    @Test
    void invalidSortIsRejectedBeforeRepositoryAccess() {
        Fixture fixture = createFixture("invalid-sort@example.com");

        assertThatThrownBy(() -> list(fixture, null, 0, 20, "assignedByTeacherId,asc"))
                .isInstanceOf(InvalidHomeworkException.class);
    }

    @Test
    void assignedPastDueHomeworkIsOverdue() {
        Fixture fixture = createFixture("past-due@example.com");
        HomeworkResult result = createHomework(
                fixture, "ДЗ", Instant.now().minusSeconds(3600), fixture.tasks()
        );

        assertThat(result.overdue()).isTrue();
    }

    @Test
    void futureDueHomeworkIsNotOverdue() {
        Fixture fixture = createFixture("future-due@example.com");
        HomeworkResult result = createHomework(
                fixture, "ДЗ", Instant.now().plusSeconds(3600), fixture.tasks()
        );

        assertThat(result.overdue()).isFalse();
    }

    @Test
    void cancelledHomeworkIsNotOverdue() {
        Fixture fixture = createFixture("cancelled-overdue@example.com");
        HomeworkResult created = createHomework(
                fixture, "ДЗ", Instant.now().minusSeconds(3600), fixture.tasks()
        );

        HomeworkResult cancelled = homeworkService.cancelHomework(
                fixture.principal(), fixture.student().getId(), created.id()
        );

        assertThat(cancelled.status()).isEqualTo(HomeworkStatus.CANCELLED);
        assertThat(cancelled.overdue()).isFalse();
    }

    @Test
    void homeworkWithoutDueAtIsNotOverdue() {
        Fixture fixture = createFixture("no-due@example.com");

        assertThat(createHomework(fixture, "ДЗ", null, fixture.tasks()).overdue()).isFalse();
    }

    @Test
    void updateChangesAllowedFieldsAndItemsButPreservesAssignmentContext() {
        Fixture fixture = createFixture("update-homework@example.com");
        HomeworkResult created = createHomework(fixture, "Old", null, fixture.tasks());
        Instant dueAt = Instant.now().plusSeconds(7200);

        HomeworkResult updated = homeworkService.updateHomework(
                fixture.principal(), fixture.student().getId(), created.id(),
                new UpdateHomeworkCommand(
                        "  New  ", "New description", dueAt, created.version(),
                        List.of(new HomeworkItemInput(fixture.secondTask().getId(), 0, false))
                )
        );

        assertThat(updated.title()).isEqualTo("New");
        assertThat(updated.description()).isEqualTo("New description");
        assertThat(updated.dueAt()).isEqualTo(dueAt);
        assertThat(updated.items()).singleElement().satisfies(item -> {
            assertThat(item.taskId()).isEqualTo(fixture.secondTask().getId());
            assertThat(item.position()).isZero();
            assertThat(item.required()).isFalse();
        });
        var persisted = homeworkRepository.findById(created.id()).orElseThrow();
        assertThat(persisted.getStudentProgramId()).isEqualTo(fixture.studentProgram().id());
        assertThat(persisted.getAssignedByTeacherId()).isEqualTo(fixture.teacher().id());
        assertThat(persisted.getAssignedAt()).isEqualTo(created.assignedAt());
    }

    @Test
    void invalidItemUpdateIsAtomic() {
        Fixture fixture = createFixture("atomic-update@example.com");
        HomeworkResult created = createHomework(fixture, "Original", null, fixture.tasks());

        assertThatThrownBy(() -> homeworkService.updateHomework(
                fixture.principal(), fixture.student().getId(), created.id(),
                new UpdateHomeworkCommand(
                        "Changed", "Changed", null, created.version(),
                        List.of(new HomeworkItemInput(UUID.randomUUID(), 0, true))
                )
        )).isInstanceOf(TaskNotFoundException.class);

        HomeworkResult unchanged = homeworkService.getHomework(
                fixture.principal(), fixture.student().getId(), created.id()
        );
        assertThat(unchanged.title()).isEqualTo("Original");
        assertThat(unchanged.items()).hasSize(2);
    }

    @Test
    void foreignHomeworkCannotBeUpdated() {
        Fixture owner = createFixture("update-owner@example.com");
        Fixture foreign = createFixture("update-foreign@example.com");
        HomeworkResult homework = createHomework(owner, "ДЗ", null, owner.tasks());

        assertThatThrownBy(() -> homeworkService.updateHomework(
                foreign.principal(), owner.student().getId(), homework.id(),
                new UpdateHomeworkCommand("X", null, null, homework.version(), itemInputs(owner.tasks()))
        )).isInstanceOf(StudentNotFoundException.class);
    }

    @Test
    void staleVersionIsRejected() {
        Fixture fixture = createFixture("version-homework@example.com");
        HomeworkResult created = createHomework(fixture, "Initial", null, fixture.tasks());
        UpdateHomeworkCommand first = new UpdateHomeworkCommand(
                "First", null, null, created.version(), itemInputs(fixture.tasks())
        );
        homeworkService.updateHomework(
                fixture.principal(), fixture.student().getId(), created.id(), first
        );

        assertThatThrownBy(() -> homeworkService.updateHomework(
                fixture.principal(), fixture.student().getId(), created.id(),
                new UpdateHomeworkCommand(
                        "Stale", null, null, created.version(), itemInputs(fixture.tasks())
                )
        )).isInstanceOf(HomeworkVersionConflictException.class);
    }

    private HomeworkResult createHomework(
            Fixture fixture,
            String title,
            Instant dueAt,
            List<TaskEntity> tasks
    ) {
        return homeworkService.createHomework(
                fixture.principal(), createCommand(fixture, title, dueAt, tasks)
        );
    }

    private CreateHomeworkCommand createCommand(
            Fixture fixture,
            String title,
            Instant dueAt,
            List<TaskEntity> tasks
    ) {
        return new CreateHomeworkCommand(
                fixture.student().getId(), fixture.studentProgram().id(), title,
                "Описание", dueAt, itemInputs(tasks)
        );
    }

    private List<HomeworkItemInput> itemInputs(List<TaskEntity> tasks) {
        return java.util.stream.IntStream.range(0, tasks.size())
                .mapToObj(index -> new HomeworkItemInput(tasks.get(index).getId(), index, true))
                .toList();
    }

    private HomeworkPageResult list(
            Fixture fixture,
            HomeworkStatus status,
            int page,
            int size,
            String sort
    ) {
        return homeworkService.listHomeworks(
                fixture.principal(), fixture.student().getId(), fixture.studentProgram().id(),
                status, page, size, sort
        );
    }

    private long homeworkCount() {
        return jdbcTemplate.queryForObject("select count(*) from homeworks", Long.class);
    }

    private Fixture createFixture(String email) {
        return createFixture(createTeacher(email), "Ученик");
    }

    private Fixture createFixture(TeacherFixture teacherFixture, String studentName) {
        StudentEntity student = studentRepository.saveAndFlush(new StudentEntity(
                UUID.randomUUID(), studentName, null, StudentStatus.ACTIVE
        ));
        teacherStudentLinkRepository.saveAndFlush(new TeacherStudentLinkEntity(
                teacherFixture.teacher(), student
        ));
        SubjectEntity subject = subjectRepository.saveAndFlush(new SubjectEntity(
                UUID.randomUUID(), teacherFixture.teacher().id(), null,
                "Предмет " + UUID.randomUUID(), null, SubjectStatus.ACTIVE
        ));
        LearningProgramEntity program = learningProgramRepository.saveAndFlush(new LearningProgramEntity(
                UUID.randomUUID(), teacherFixture.teacher().id(), subject.id(),
                "Программа", null, LearningProgramStatus.ACTIVE
        ));
        StudentProgramEntity studentProgram = studentProgramRepository.saveAndFlush(new StudentProgramEntity(
                UUID.randomUUID(), student.getId(), program.getId(), teacherFixture.teacher().id(),
                StudentProgramStatus.ACTIVE, 480, Instant.now(), null
        ));
        TaskEntity first = createTask(
                teacherFixture.teacher(), subject, "Первая задача", TaskType.TEXT, TaskStatus.ACTIVE
        );
        TaskEntity second = createTask(
                teacherFixture.teacher(), subject, "Вторая задача", TaskType.TEXT, TaskStatus.ACTIVE
        );
        return new Fixture(
                teacherFixture.teacher(), teacherFixture.principal(), student, subject,
                studentProgram, first, second
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
                user.id(), email, "password-hash", true,
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );
        return new TeacherFixture(teacher, principal);
    }

    private TaskEntity createTask(
            TeacherEntity teacher,
            SubjectEntity subject,
            String title,
            TaskType type,
            TaskStatus status
    ) {
        return taskRepository.saveAndFlush(new TaskEntity(
                UUID.randomUUID(), teacher.id(), subject.id(), title, "Условие",
                type, TaskDifficulty.EASY, status
        ));
    }

    private record TeacherFixture(TeacherEntity teacher, AuthenticatedUser principal) {
    }

    private record Fixture(
            TeacherEntity teacher,
            AuthenticatedUser principal,
            StudentEntity student,
            SubjectEntity subject,
            StudentProgramEntity studentProgram,
            TaskEntity firstTask,
            TaskEntity secondTask
    ) {
        List<TaskEntity> tasks() {
            return List.of(firstTask, secondTask);
        }
    }
}
