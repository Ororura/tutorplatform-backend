package com.tutorplatform.content.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.tutorplatform.content.domain.*;
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
    JpaFileAssetRepository.class,
    JpaLessonMaterialRepository.class
})
class MaterialPersistenceIntegrationTest extends PostgresIntegrationTest {

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_material_persistence", "008");
    }

    @Autowired private UserRepository userRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private LearningProgramRepository learningProgramRepository;
    @Autowired private ModuleRepository moduleRepository;
    @Autowired private TopicRepository topicRepository;
    @Autowired private FileAssetRepository fileAssetRepository;
    @Autowired private LessonMaterialRepository lessonMaterialRepository;
    @Autowired private Flyway flyway;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void flywayV004AppliesSuccessfully() {
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
                  and table_name in ('file_assets', 'lesson_materials')
                """,
                                Integer.class))
                .isEqualTo(2);
    }

    @Test
    void textMaterialIsSaved() {
        assertTextMaterialSaved(LessonMaterialType.TEXT, "Конспект", "Текст урока");
    }

    @Test
    void markdownMaterialIsSaved() {
        assertTextMaterialSaved(LessonMaterialType.MARKDOWN, "Markdown", "# Заголовок");
    }

    @Test
    void codeExampleMaterialIsSaved() {
        assertTextMaterialSaved(LessonMaterialType.CODE_EXAMPLE, "Пример", "System.out.println();");
    }

    @Test
    void linkMaterialRequiresExternalUrl() {
        ContentFixture fixture = createContentFixture("link-material@example.com");
        LessonMaterialEntity link =
                lessonMaterialRepository.saveAndFlush(
                        new LessonMaterialEntity(
                                UUID.randomUUID(),
                                fixture.topic().id(),
                                fixture.teacher().id(),
                                LessonMaterialType.LINK,
                                "Документация",
                                null,
                                null,
                                "https://example.com/docs",
                                0));

        assertThat(lessonMaterialRepository.findById(link.getId()))
                .get()
                .extracting(LessonMaterialEntity::getExternalUrl)
                .isEqualTo("https://example.com/docs");
        assertThatThrownBy(() -> insertMaterialWithoutRequiredValue(fixture, "LINK", 1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void fileMaterialRequiresFileAsset() {
        ContentFixture fixture = createContentFixture("file-material@example.com");
        FileAssetEntity fileAsset = createFileAsset(fixture.teacher(), "files/file-material");
        LessonMaterialEntity material =
                lessonMaterialRepository.saveAndFlush(
                        new LessonMaterialEntity(
                                UUID.randomUUID(),
                                fixture.topic().id(),
                                fixture.teacher().id(),
                                LessonMaterialType.FILE,
                                "Файл",
                                null,
                                fileAsset.id(),
                                null,
                                0));

        assertThat(material.getFileAssetId()).isEqualTo(fileAsset.id());
        assertThatThrownBy(() -> insertMaterialWithoutRequiredValue(fixture, "FILE", 1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void imageMaterialRequiresFileAsset() {
        ContentFixture fixture = createContentFixture("image-material@example.com");
        FileAssetEntity fileAsset = createFileAsset(fixture.teacher(), "images/image-material");
        LessonMaterialEntity material =
                lessonMaterialRepository.saveAndFlush(
                        new LessonMaterialEntity(
                                UUID.randomUUID(),
                                fixture.topic().id(),
                                fixture.teacher().id(),
                                LessonMaterialType.IMAGE,
                                "Схема",
                                null,
                                fileAsset.id(),
                                null,
                                0));

        assertThat(material.getFileAssetId()).isEqualTo(fileAsset.id());
        assertThatThrownBy(() -> insertMaterialWithoutRequiredValue(fixture, "IMAGE", 1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void negativeMaterialPositionIsRejected() {
        ContentFixture fixture = createContentFixture("negative-position@example.com");

        assertThatThrownBy(
                        () ->
                                jdbcTemplate.update(
                                        """
                insert into lesson_materials (
                    id, topic_id, created_by_teacher_id, material_type, title, content, position
                ) values (?, ?, ?, 'TEXT', 'Текст', 'Содержимое', -1)
                """,
                                        UUID.randomUUID(),
                                        fixture.topic().id(),
                                        fixture.teacher().id()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateTopicPositionIsRejected() {
        ContentFixture fixture = createContentFixture("duplicate-position@example.com");
        createTextMaterial(fixture, 0, "Первый");

        assertThatThrownBy(() -> createTextMaterial(fixture, 0, "Второй"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void lessonMaterialIsLinkedToTopic() {
        ContentFixture fixture = createContentFixture("material-topic@example.com");
        LessonMaterialEntity material = createTextMaterial(fixture, 0, "Материал");

        assertThat(lessonMaterialRepository.findById(material.getId()))
                .get()
                .extracting(LessonMaterialEntity::getTopicId)
                .isEqualTo(fixture.topic().id());
        assertThat(
                        lessonMaterialRepository.existsByIdAndTopicId(
                                material.getId(), fixture.topic().id()))
                .isTrue();
        assertThat(
                        lessonMaterialRepository.existsByIdAndTopicId(
                                material.getId(), UUID.randomUUID()))
                .isFalse();
    }

    @Test
    void materialsAreListedByTopicInPositionOrder() {
        ContentFixture fixture = createContentFixture("material-order@example.com");
        LessonMaterialEntity second = createTextMaterial(fixture, 1, "Второй");
        LessonMaterialEntity first = createTextMaterial(fixture, 0, "Первый");

        assertThat(lessonMaterialRepository.findAllByTopicIdOrderByPosition(fixture.topic().id()))
                .extracting(LessonMaterialEntity::getId)
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    void fileAssetIsLinkedToTeacher() {
        ContentFixture fixture = createContentFixture("asset-teacher@example.com");
        FileAssetEntity fileAsset = createFileAsset(fixture.teacher(), "files/teacher-link");

        assertThat(fileAssetRepository.findById(fileAsset.id()))
                .get()
                .extracting(FileAssetEntity::uploadedByTeacherId)
                .isEqualTo(fixture.teacher().id());
    }

    @Test
    void storageKeyIsUnique() {
        ContentFixture fixture = createContentFixture("unique-storage-key@example.com");
        createFileAsset(fixture.teacher(), "files/unique");

        assertThatThrownBy(() -> createFileAsset(fixture.teacher(), "files/unique"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void negativeFileSizeIsRejected() {
        ContentFixture fixture = createContentFixture("negative-file-size@example.com");

        assertThatThrownBy(
                        () ->
                                jdbcTemplate.update(
                                        """
                insert into file_assets (
                    id, uploaded_by_teacher_id, storage_provider, storage_key,
                    original_filename, mime_type, size_bytes
                ) values (?, ?, 'LOCAL', ?, 'file.txt', 'text/plain', -1)
                """,
                                        UUID.randomUUID(),
                                        fixture.teacher().id(),
                                        "files/negative-" + UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void lessonMaterialOptimisticLockingWorks() {
        ContentFixture fixture = createContentFixture("material-lock@example.com");
        LessonMaterialEntity material = createTextMaterial(fixture, 0, "Исходный");
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        LessonMaterialEntity first =
                transaction.execute(
                        status ->
                                lessonMaterialRepository.findById(material.getId()).orElseThrow());
        LessonMaterialEntity stale =
                transaction.execute(
                        status ->
                                lessonMaterialRepository.findById(material.getId()).orElseThrow());

        transaction.executeWithoutResult(
                status -> {
                    first.update(LessonMaterialType.TEXT, "Первая версия", "Текст", null, null, 0);
                    lessonMaterialRepository.saveAndFlush(first);
                });
        Throwable thrown =
                catchThrowable(
                        () ->
                                transaction.executeWithoutResult(
                                        status -> {
                                            stale.update(
                                                    LessonMaterialType.TEXT,
                                                    "Устаревшая версия",
                                                    "Текст",
                                                    null,
                                                    null,
                                                    0);
                                            lessonMaterialRepository.saveAndFlush(stale);
                                        }));

        assertThat(thrown).isNotNull();
        assertThat(hasOptimisticLockCause(thrown)).isTrue();
        assertThat(lessonMaterialRepository.findById(material.getId()))
                .get()
                .extracting(LessonMaterialEntity::getTitle)
                .isEqualTo("Первая версия");
    }

    private void assertTextMaterialSaved(LessonMaterialType type, String title, String content) {
        ContentFixture fixture = createContentFixture(type.name().toLowerCase() + "@example.com");
        LessonMaterialEntity material =
                lessonMaterialRepository.saveAndFlush(
                        new LessonMaterialEntity(
                                UUID.randomUUID(),
                                fixture.topic().id(),
                                fixture.teacher().id(),
                                type,
                                title,
                                content,
                                null,
                                null,
                                0));

        assertThat(lessonMaterialRepository.findById(material.getId()))
                .get()
                .satisfies(
                        persisted -> {
                            assertThat(persisted.getMaterialType()).isEqualTo(type);
                            assertThat(persisted.getContent()).isEqualTo(content);
                            assertThat(persisted.getVersion()).isZero();
                            assertThat(persisted.getCreatedAt()).isNotNull();
                            assertThat(persisted.getUpdatedAt()).isNotNull();
                        });
    }

    private LessonMaterialEntity createTextMaterial(
            ContentFixture fixture, int position, String title) {
        return lessonMaterialRepository.saveAndFlush(
                new LessonMaterialEntity(
                        UUID.randomUUID(),
                        fixture.topic().id(),
                        fixture.teacher().id(),
                        LessonMaterialType.TEXT,
                        title,
                        "Содержимое",
                        null,
                        null,
                        position));
    }

    private FileAssetEntity createFileAsset(TeacherEntity teacher, String storageKey) {
        return fileAssetRepository.saveAndFlush(
                new FileAssetEntity(
                        UUID.randomUUID(),
                        teacher.id(),
                        StorageProvider.LOCAL,
                        storageKey,
                        "file.txt",
                        "text/plain",
                        128,
                        null));
    }

    private void insertMaterialWithoutRequiredValue(
            ContentFixture fixture, String type, int position) {
        jdbcTemplate.update(
                """
                insert into lesson_materials (
                    id, topic_id, created_by_teacher_id, material_type, title, position
                ) values (?, ?, ?, ?, 'Материал', ?)
                """,
                UUID.randomUUID(),
                fixture.topic().id(),
                fixture.teacher().id(),
                type,
                position);
    }

    private ContentFixture createContentFixture(String email) {
        UserEntity user =
                new UserEntity(UUID.randomUUID(), email, "password-hash", UserStatus.ACTIVE);
        user.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(user);
        TeacherEntity teacher =
                teacherRepository.saveAndFlush(
                        new TeacherEntity(UUID.randomUUID(), user, "Teacher"));
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
        return new ContentFixture(teacher, topic);
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

    private record ContentFixture(TeacherEntity teacher, TopicEntity topic) {}
}
