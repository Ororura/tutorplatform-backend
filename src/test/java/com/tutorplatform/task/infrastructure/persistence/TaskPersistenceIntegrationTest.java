package com.tutorplatform.task.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.tutorplatform.program.domain.*;
import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import com.tutorplatform.program.infrastructure.persistence.JpaModuleRepository;
import com.tutorplatform.program.infrastructure.persistence.JpaTopicRepository;
import com.tutorplatform.program.infrastructure.persistence.learningprogram.JpaLearningProgramRepository;
import com.tutorplatform.subject.domain.SubjectEntity;
import com.tutorplatform.subject.domain.SubjectRepository;
import com.tutorplatform.subject.domain.SubjectStatus;
import com.tutorplatform.subject.infrastructure.persistence.JpaSubjectRepository;
import com.tutorplatform.task.domain.skill.SkillEntity;
import com.tutorplatform.task.domain.skill.SkillRepository;
import com.tutorplatform.task.domain.task.*;
import com.tutorplatform.task.domain.taskskill.TaskSkillEntity;
import com.tutorplatform.task.domain.taskskill.TaskSkillRepository;
import com.tutorplatform.task.domain.topic.TopicTaskEntity;
import com.tutorplatform.task.domain.topic.TopicTaskRepository;
import com.tutorplatform.task.infrastructure.persistence.skill.JpaSkillRepository;
import com.tutorplatform.task.infrastructure.persistence.task.JpaTaskRepository;
import com.tutorplatform.task.infrastructure.persistence.taskskill.JpaTaskSkillRepository;
import com.tutorplatform.task.infrastructure.persistence.topic.JpaTopicTaskRepository;
import com.tutorplatform.test.PostgresIntegrationTest;
import com.tutorplatform.user.domain.*;
import com.tutorplatform.user.infrastructure.persistence.JpaTeacherRepository;
import com.tutorplatform.user.infrastructure.persistence.JpaUserRepository;
import jakarta.persistence.OptimisticLockException;
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
    JpaSubjectRepository.class,
    JpaLearningProgramRepository.class,
    JpaModuleRepository.class,
    JpaTopicRepository.class,
    JpaTaskRepository.class,
    JpaTopicTaskRepository.class,
    JpaSkillRepository.class,
    JpaTaskSkillRepository.class
})
class TaskPersistenceIntegrationTest extends PostgresIntegrationTest {

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_task_persistence", "008");
    }

    @Autowired private UserRepository userRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private LearningProgramRepository learningProgramRepository;
    @Autowired private ModuleRepository moduleRepository;
    @Autowired private TopicRepository topicRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private TopicTaskRepository topicTaskRepository;
    @Autowired private SkillRepository skillRepository;
    @Autowired private TaskSkillRepository taskSkillRepository;
    @Autowired private Flyway flyway;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void flywayMigrationV006AppliesSuccessfully() {
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
                    'tasks', 'topic_tasks', 'skills', 'task_skills',
                    'programming_task_configs', 'task_test_cases'
                  )
                """,
                                Integer.class))
                .isEqualTo(6);
    }

    @Test
    void textTaskIsSaved() {
        TaskFixture fixture =
                createTaskFixture(TaskType.TEXT, TaskDifficulty.EASY, TaskStatus.DRAFT);

        TaskEntity persisted = taskRepository.findById(fixture.task().getId()).orElseThrow();

        assertThat(persisted.getTitle()).isEqualTo("Текстовое задание");
        assertThat(persisted.getDescriptionMarkdown()).isEqualTo("**Условие**");
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getUpdatedAt()).isNotNull();
        assertThat(persisted.getVersion()).isZero();
    }

    @Test
    void taskTypeIsPersisted() {
        TaskFixture fixture =
                createTaskFixture(TaskType.TEXT, TaskDifficulty.EASY, TaskStatus.DRAFT);

        assertThat(taskRepository.findById(fixture.task().getId()))
                .get()
                .extracting(TaskEntity::getTaskType)
                .isEqualTo(TaskType.TEXT);
    }

    @Test
    void taskDifficultyIsPersisted() {
        TaskFixture fixture =
                createTaskFixture(TaskType.TEXT, TaskDifficulty.HARD, TaskStatus.DRAFT);

        assertThat(taskRepository.findById(fixture.task().getId()))
                .get()
                .extracting(TaskEntity::getDifficulty)
                .isEqualTo(TaskDifficulty.HARD);
    }

    @Test
    void taskStatusIsPersisted() {
        TaskFixture fixture =
                createTaskFixture(TaskType.TEXT, TaskDifficulty.MEDIUM, TaskStatus.ACTIVE);

        assertThat(taskRepository.findById(fixture.task().getId()))
                .get()
                .extracting(TaskEntity::getStatus)
                .isEqualTo(TaskStatus.ACTIVE);
    }

    @Test
    void taskIsLinkedToTeacher() {
        TaskFixture fixture =
                createTaskFixture(TaskType.TEXT, TaskDifficulty.EASY, TaskStatus.DRAFT);

        assertThat(taskRepository.findById(fixture.task().getId()))
                .get()
                .extracting(TaskEntity::getTeacherId)
                .isEqualTo(fixture.teacher().id());
        assertThat(taskRepository.findOwnedById(fixture.task().getId(), fixture.teacher().id()))
                .isPresent();
        assertThat(taskRepository.findOwnedById(fixture.task().getId(), UUID.randomUUID()))
                .isEmpty();
    }

    @Test
    void taskIsLinkedToSubjectAndTeacherListCanBeFiltered() {
        TaskFixture fixture =
                createTaskFixture(TaskType.TEXT, TaskDifficulty.EASY, TaskStatus.ACTIVE);

        assertThat(taskRepository.findById(fixture.task().getId()))
                .get()
                .extracting(TaskEntity::getSubjectId)
                .isEqualTo(fixture.subject().id());
        assertThat(
                        taskRepository.findAllByTeacherId(
                                fixture.teacher().id(),
                                fixture.subject().id(),
                                TaskStatus.ACTIVE,
                                TaskType.TEXT))
                .extracting(TaskEntity::getId)
                .containsExactly(fixture.task().getId());
        assertThat(
                        taskRepository.findAllByTeacherId(
                                fixture.teacher().id(),
                                fixture.subject().id(),
                                TaskStatus.DRAFT,
                                TaskType.TEXT))
                .isEmpty();
    }

    @Test
    void topicTaskLinksTopicAndTaskInPositionOrder() {
        TaskFixture fixture =
                createTaskFixture(TaskType.TEXT, TaskDifficulty.EASY, TaskStatus.DRAFT);
        TaskEntity secondTask = createTask(fixture.teacher(), fixture.subject(), "Второе задание");
        TopicEntity topic = createTopic(fixture.learningProgram());

        topicTaskRepository.saveAndFlush(
                new TopicTaskEntity(topic.id(), secondTask.getId(), 1, false));
        topicTaskRepository.saveAndFlush(
                new TopicTaskEntity(topic.id(), fixture.task().getId(), 0, true));

        assertThat(topicTaskRepository.findAllByTopicIdOrderByPosition(topic.id()))
                .extracting(TopicTaskEntity::taskId)
                .containsExactly(fixture.task().getId(), secondTask.getId());
    }

    @Test
    void duplicateTopicTaskIsRejected() {
        TaskFixture fixture =
                createTaskFixture(TaskType.TEXT, TaskDifficulty.EASY, TaskStatus.DRAFT);
        TopicEntity topic = createTopic(fixture.learningProgram());
        topicTaskRepository.saveAndFlush(
                new TopicTaskEntity(topic.id(), fixture.task().getId(), 0, true));

        assertThatThrownBy(
                        () ->
                                jdbcTemplate.update(
                                        "insert into topic_tasks(topic_id, task_id, position) values (?, ?, ?)",
                                        topic.id(),
                                        fixture.task().getId(),
                                        1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicatePositionWithinTopicIsRejected() {
        TaskFixture fixture =
                createTaskFixture(TaskType.TEXT, TaskDifficulty.EASY, TaskStatus.DRAFT);
        TaskEntity secondTask = createTask(fixture.teacher(), fixture.subject(), "Второе задание");
        TopicEntity topic = createTopic(fixture.learningProgram());
        topicTaskRepository.saveAndFlush(
                new TopicTaskEntity(topic.id(), fixture.task().getId(), 0, true));

        assertThatThrownBy(
                        () ->
                                topicTaskRepository.saveAndFlush(
                                        new TopicTaskEntity(
                                                topic.id(), secondTask.getId(), 0, true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void negativeTopicTaskPositionIsRejectedByDatabase() {
        TaskFixture fixture =
                createTaskFixture(TaskType.TEXT, TaskDifficulty.EASY, TaskStatus.DRAFT);
        TopicEntity topic = createTopic(fixture.learningProgram());

        assertThatThrownBy(
                        () ->
                                jdbcTemplate.update(
                                        "insert into topic_tasks(topic_id, task_id, position) values (?, ?, ?)",
                                        topic.id(),
                                        fixture.task().getId(),
                                        -1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void skillIsUniqueBySubjectAndCode() {
        TaskFixture fixture =
                createTaskFixture(TaskType.TEXT, TaskDifficulty.EASY, TaskStatus.DRAFT);
        skillRepository.saveAndFlush(
                new SkillEntity(UUID.randomUUID(), fixture.subject().id(), "LOOPS", "Циклы", null));

        assertThatThrownBy(
                        () ->
                                skillRepository.saveAndFlush(
                                        new SkillEntity(
                                                UUID.randomUUID(),
                                                fixture.subject().id(),
                                                "LOOPS",
                                                "Другие циклы",
                                                null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void taskSkillCompositePrimaryKeyWorks() {
        TaskFixture fixture =
                createTaskFixture(TaskType.TEXT, TaskDifficulty.EASY, TaskStatus.DRAFT);
        SkillEntity first = createSkill(fixture.subject(), "LOOPS");
        SkillEntity second = createSkill(fixture.subject(), "CONDITIONS");
        taskSkillRepository.saveAndFlush(new TaskSkillEntity(fixture.task().getId(), first.id()));
        taskSkillRepository.saveAndFlush(new TaskSkillEntity(fixture.task().getId(), second.id()));

        assertThat(taskSkillRepository.findAllByTaskId(fixture.task().getId()))
                .extracting(TaskSkillEntity::skillId)
                .containsExactlyInAnyOrder(first.id(), second.id());
        assertThatThrownBy(
                        () ->
                                jdbcTemplate.update(
                                        "insert into task_skills(task_id, skill_id) values (?, ?)",
                                        fixture.task().getId(),
                                        first.id()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void taskOptimisticLockingWorks() {
        TaskFixture fixture =
                createTaskFixture(TaskType.TEXT, TaskDifficulty.EASY, TaskStatus.DRAFT);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        TaskEntity first =
                transaction.execute(
                        status -> taskRepository.findById(fixture.task().getId()).orElseThrow());
        TaskEntity stale =
                transaction.execute(
                        status -> taskRepository.findById(fixture.task().getId()).orElseThrow());

        first.update(
                "Первая версия",
                first.getDescriptionMarkdown(),
                first.getTaskType(),
                first.getDifficulty(),
                first.getStatus());
        transaction.executeWithoutResult(status -> taskRepository.saveAndFlush(first));

        stale.update(
                "Устаревшая версия",
                stale.getDescriptionMarkdown(),
                stale.getTaskType(),
                stale.getDifficulty(),
                stale.getStatus());
        Throwable thrown =
                catchThrowable(
                        () ->
                                transaction.executeWithoutResult(
                                        status -> taskRepository.saveAndFlush(stale)));

        assertThat(thrown).isNotNull();
        assertThat(hasOptimisticLockCause(thrown)).isTrue();
        assertThat(taskRepository.findById(fixture.task().getId()))
                .get()
                .extracting(TaskEntity::getTitle)
                .isEqualTo("Первая версия");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void taskDeletionCascadesOwnedRowsAndReferencedSkillRemainsRestricted() {
        TaskFixture fixture =
                createTaskFixture(TaskType.CODE, TaskDifficulty.EASY, TaskStatus.DRAFT);
        TopicEntity topic = createTopic(fixture.learningProgram());
        SkillEntity skill = createSkill(fixture.subject(), "LOOPS");
        topicTaskRepository.saveAndFlush(
                new TopicTaskEntity(topic.id(), fixture.task().getId(), 0, true));
        taskSkillRepository.saveAndFlush(new TaskSkillEntity(fixture.task().getId(), skill.id()));
        UUID testCaseId = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into programming_task_configs(task_id, language) values (?, 'PYTHON')",
                fixture.task().getId());
        jdbcTemplate.update(
                "insert into task_test_cases(id, task_id, expected_output, position) values (?, ?, '', 0)",
                testCaseId,
                fixture.task().getId());

        assertThatThrownBy(() -> jdbcTemplate.update("delete from skills where id = ?", skill.id()))
                .isInstanceOf(DataIntegrityViolationException.class);

        jdbcTemplate.update("delete from tasks where id = ?", fixture.task().getId());

        assertThat(
                        jdbcTemplate.queryForObject(
                                "select count(*) from topic_tasks where task_id = ?",
                                Integer.class,
                                fixture.task().getId()))
                .isZero();
        assertThat(
                        jdbcTemplate.queryForObject(
                                "select count(*) from task_skills where task_id = ?",
                                Integer.class,
                                fixture.task().getId()))
                .isZero();
        assertThat(
                        jdbcTemplate.queryForObject(
                                "select count(*) from programming_task_configs where task_id = ?",
                                Integer.class,
                                fixture.task().getId()))
                .isZero();
        assertThat(
                        jdbcTemplate.queryForObject(
                                "select count(*) from task_test_cases where task_id = ?",
                                Integer.class,
                                fixture.task().getId()))
                .isZero();
        assertThat(skillRepository.findById(skill.id())).isPresent();
    }

    private TaskFixture createTaskFixture(
            TaskType taskType, TaskDifficulty difficulty, TaskStatus status) {
        TeacherEntity teacher = createTeacher();
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
        TaskEntity task =
                taskRepository.saveAndFlush(
                        new TaskEntity(
                                UUID.randomUUID(),
                                teacher.id(),
                                subject.id(),
                                "Текстовое задание",
                                "**Условие**",
                                taskType,
                                difficulty,
                                status));
        return new TaskFixture(teacher, subject, learningProgram, task);
    }

    private TeacherEntity createTeacher() {
        UserEntity user =
                new UserEntity(
                        UUID.randomUUID(),
                        UUID.randomUUID() + "@example.com",
                        "password-hash",
                        UserStatus.ACTIVE);
        user.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(user);
        return teacherRepository.saveAndFlush(
                new TeacherEntity(UUID.randomUUID(), user, "Teacher"));
    }

    private TaskEntity createTask(TeacherEntity teacher, SubjectEntity subject, String title) {
        return taskRepository.saveAndFlush(
                new TaskEntity(
                        UUID.randomUUID(),
                        teacher.id(),
                        subject.id(),
                        title,
                        "Условие",
                        TaskType.TEXT,
                        TaskDifficulty.EASY,
                        TaskStatus.DRAFT));
    }

    private TopicEntity createTopic(LearningProgramEntity learningProgram) {
        ModuleEntity module =
                moduleRepository.saveAndFlush(
                        new ModuleEntity(
                                UUID.randomUUID(), learningProgram.getId(), "Модуль", null, 0));
        return topicRepository.saveAndFlush(
                new TopicEntity(
                        UUID.randomUUID(), module.id(), "Тема", null, 0, TopicStatus.DRAFT));
    }

    private SkillEntity createSkill(SubjectEntity subject, String code) {
        return skillRepository.saveAndFlush(
                new SkillEntity(UUID.randomUUID(), subject.id(), code, "Навык " + code, null));
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

    private record TaskFixture(
            TeacherEntity teacher,
            SubjectEntity subject,
            LearningProgramEntity learningProgram,
            TaskEntity task) {}
}
