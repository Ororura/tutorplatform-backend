package com.tutorplatform.content.application;

import com.tutorplatform.test.PostgresIntegrationTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.application.exception.InvalidLessonMaterialException;
import com.tutorplatform.content.application.exception.LessonMaterialNotFoundException;
import com.tutorplatform.content.application.exception.LessonMaterialPositionConflictException;
import com.tutorplatform.content.application.exception.TopicNotFoundException;
import com.tutorplatform.content.domain.LessonMaterialType;
import com.tutorplatform.program.domain.*;
import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class LessonMaterialApplicationIntegrationTest extends PostgresIntegrationTest {

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_lesson_material_application", "008");
    }

    @Autowired
    private LessonMaterialService lessonMaterialService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private LearningProgramRepository learningProgramRepository;
    @Autowired
    private ModuleRepository moduleRepository;
    @Autowired
    private TopicRepository topicRepository;

    @Test
    void teacherCreatesTextMaterial() {
        ContentFixture fixture = createFixture("create-text-material@example.com");

        LessonMaterialResult created = createMaterial(
            fixture, LessonMaterialType.TEXT, "Текст", "Содержимое", null, 0
        );

        assertThat(created.materialType()).isEqualTo(LessonMaterialType.TEXT);
        assertThat(created.content()).isEqualTo("Содержимое");
        assertThat(created.createdByTeacherId()).isEqualTo(fixture.teacher().id());
        assertThat(created.version()).isZero();
    }

    @Test
    void teacherCreatesMarkdownMaterial() {
        ContentFixture fixture = createFixture("create-markdown-material@example.com");

        LessonMaterialResult created = createMaterial(
            fixture, LessonMaterialType.MARKDOWN, "Markdown", "# Заголовок", null, 0
        );

        assertThat(created.materialType()).isEqualTo(LessonMaterialType.MARKDOWN);
        assertThat(created.content()).isEqualTo("# Заголовок");
    }

    @Test
    void teacherCreatesCodeExampleMaterial() {
        ContentFixture fixture = createFixture("create-code-material@example.com");

        LessonMaterialResult created = createMaterial(
            fixture, LessonMaterialType.CODE_EXAMPLE, "Пример", "System.out.println();", null, 0
        );

        assertThat(created.materialType()).isEqualTo(LessonMaterialType.CODE_EXAMPLE);
        assertThat(created.content()).isEqualTo("System.out.println();");
    }

    @Test
    void teacherCreatesLinkMaterial() {
        ContentFixture fixture = createFixture("create-link-material@example.com");

        LessonMaterialResult created = createMaterial(
            fixture, LessonMaterialType.LINK, "Документация", null,
            "https://example.com/docs", 0
        );

        assertThat(created.materialType()).isEqualTo(LessonMaterialType.LINK);
        assertThat(created.externalUrl()).isEqualTo("https://example.com/docs");
        assertThat(created.content()).isNull();
        assertThat(created.fileAssetId()).isNull();
    }

    @Test
    void topicOwnedByAnotherTeacherIsRejected() {
        ContentFixture current = createFixture("current-topic-owner@example.com");
        ContentFixture foreign = createFixture("foreign-topic-owner@example.com");

        assertThatThrownBy(() -> lessonMaterialService.createLessonMaterial(
            current.principal(),
            createCommand(foreign.topic().id(), LessonMaterialType.TEXT, "Текст", "Содержимое", null, 0)
        )).isInstanceOf(TopicNotFoundException.class);
    }

    @Test
    void unknownTopicIsRejected() {
        ContentFixture fixture = createFixture("unknown-topic@example.com");

        assertThatThrownBy(() -> lessonMaterialService.createLessonMaterial(
            fixture.principal(),
            createCommand(UUID.randomUUID(), LessonMaterialType.TEXT, "Текст", "Содержимое", null, 0)
        )).isInstanceOf(TopicNotFoundException.class);
    }

    @Test
    void textWithoutContentIsRejected() {
        ContentFixture fixture = createFixture("text-without-content@example.com");

        assertThatThrownBy(() -> lessonMaterialService.createLessonMaterial(
            fixture.principal(),
            createCommand(fixture.topic().id(), LessonMaterialType.TEXT, "Текст", null, null, 0)
        )).isInstanceOf(InvalidLessonMaterialException.class)
            .extracting("field")
            .isEqualTo("content");
    }

    @Test
    void linkWithoutExternalUrlIsRejected() {
        ContentFixture fixture = createFixture("link-without-url@example.com");

        assertThatThrownBy(() -> lessonMaterialService.createLessonMaterial(
            fixture.principal(),
            createCommand(fixture.topic().id(), LessonMaterialType.LINK, "Ссылка", null, null, 0)
        )).isInstanceOf(InvalidLessonMaterialException.class)
            .extracting("field")
            .isEqualTo("externalUrl");
    }

    @Test
    void negativePositionIsRejected() {
        ContentFixture fixture = createFixture("negative-app-position@example.com");

        assertThatThrownBy(() -> lessonMaterialService.createLessonMaterial(
            fixture.principal(),
            createCommand(fixture.topic().id(), LessonMaterialType.TEXT, "Текст", "Содержимое", null, -1)
        )).isInstanceOf(InvalidLessonMaterialException.class)
            .extracting("field")
            .isEqualTo("position");
    }

    @Test
    void duplicatePositionIsReportedAsConflict() {
        ContentFixture fixture = createFixture("duplicate-app-position@example.com");
        createTextMaterial(fixture, "Первый", 0);

        assertThatThrownBy(() -> createTextMaterial(fixture, "Второй", 0))
            .isInstanceOf(LessonMaterialPositionConflictException.class);
    }

    @Test
    void listIsOrderedByPosition() {
        ContentFixture fixture = createFixture("list-materials@example.com");
        LessonMaterialResult third = createTextMaterial(fixture, "Третий", 2);
        LessonMaterialResult first = createTextMaterial(fixture, "Первый", 0);
        LessonMaterialResult second = createTextMaterial(fixture, "Второй", 1);

        assertThat(lessonMaterialService.listLessonMaterials(
            fixture.principal(), fixture.topic().id()
        )).extracting(LessonMaterialResult::id)
            .containsExactly(first.id(), second.id(), third.id());
    }

    @Test
    void getDoesNotRevealForeignMaterial() {
        ContentFixture current = createFixture("current-material-reader@example.com");
        ContentFixture foreign = createFixture("foreign-material-reader@example.com");
        LessonMaterialResult foreignMaterial = createTextMaterial(foreign, "Чужой", 0);

        assertThatThrownBy(() -> lessonMaterialService.getLessonMaterial(
            current.principal(), current.topic().id(), foreignMaterial.id()
        )).isInstanceOf(LessonMaterialNotFoundException.class);
    }

    @Test
    void updateChangesAllowedFieldsAndMaterialType() {
        ContentFixture fixture = createFixture("update-material@example.com");
        LessonMaterialResult created = createTextMaterial(fixture, "Текст", 0);

        LessonMaterialResult updated = lessonMaterialService.updateLessonMaterial(
            fixture.principal(),
            fixture.topic().id(),
            created.id(),
            new UpdateLessonMaterialCommand(
                LessonMaterialType.LINK,
                "Новая ссылка",
                null,
                null,
                "https://example.com/updated",
                3,
                created.version()
            )
        );

        assertThat(updated.materialType()).isEqualTo(LessonMaterialType.LINK);
        assertThat(updated.title()).isEqualTo("Новая ссылка");
        assertThat(updated.content()).isNull();
        assertThat(updated.externalUrl()).isEqualTo("https://example.com/updated");
        assertThat(updated.position()).isEqualTo(3);
        assertThat(updated.version()).isEqualTo(created.version() + 1);
        assertThat(updated.topicId()).isEqualTo(created.topicId());
        assertThat(updated.createdByTeacherId()).isEqualTo(created.createdByTeacherId());
    }

    @Test
    void typeChangeRejectsStalePayload() {
        ContentFixture fixture = createFixture("invalid-type-change@example.com");
        LessonMaterialResult created = createTextMaterial(fixture, "Текст", 0);

        assertThatThrownBy(() -> lessonMaterialService.updateLessonMaterial(
            fixture.principal(),
            fixture.topic().id(),
            created.id(),
            new UpdateLessonMaterialCommand(
                LessonMaterialType.LINK,
                "Ссылка",
                created.content(),
                null,
                "https://example.com",
                0,
                created.version()
            )
        )).isInstanceOf(InvalidLessonMaterialException.class);

        assertThat(lessonMaterialService.getLessonMaterial(
            fixture.principal(), fixture.topic().id(), created.id()
        ).materialType()).isEqualTo(LessonMaterialType.TEXT);
    }

    @Test
    void fileAndImageCreationAreNotSupported() {
        ContentFixture fixture = createFixture("unsupported-file-types@example.com");

        assertThatThrownBy(() -> lessonMaterialService.createLessonMaterial(
            fixture.principal(),
            new CreateLessonMaterialCommand(
                fixture.topic().id(), LessonMaterialType.FILE, "Файл", null,
                UUID.randomUUID(), null, 0
            )
        )).isInstanceOf(InvalidLessonMaterialException.class)
            .extracting("field")
            .isEqualTo("materialType");
        assertThatThrownBy(() -> lessonMaterialService.createLessonMaterial(
            fixture.principal(),
            new CreateLessonMaterialCommand(
                fixture.topic().id(), LessonMaterialType.IMAGE, "Изображение", null,
                UUID.randomUUID(), null, 0
            )
        )).isInstanceOf(InvalidLessonMaterialException.class)
            .extracting("field")
            .isEqualTo("materialType");
    }

    @Test
    void blankTitleIsRejected() {
        ContentFixture fixture = createFixture("blank-material-title@example.com");

        assertThatThrownBy(() -> lessonMaterialService.createLessonMaterial(
            fixture.principal(),
            createCommand(fixture.topic().id(), LessonMaterialType.TEXT, " ", "Содержимое", null, 0)
        )).isInstanceOf(InvalidLessonMaterialException.class)
            .extracting("field")
            .isEqualTo("title");
    }

    @Test
    void optimisticLockingRejectsStaleUpdate() {
        ContentFixture fixture = createFixture("application-material-lock@example.com");
        LessonMaterialResult created = createTextMaterial(fixture, "Исходный", 0);
        UpdateLessonMaterialCommand firstUpdate = updateTextCommand(created, "Первая версия");
        UpdateLessonMaterialCommand staleUpdate = updateTextCommand(created, "Устаревшая версия");

        LessonMaterialResult updated = lessonMaterialService.updateLessonMaterial(
            fixture.principal(), fixture.topic().id(), created.id(), firstUpdate
        );
        Throwable thrown = catchThrowable(() -> lessonMaterialService.updateLessonMaterial(
            fixture.principal(), fixture.topic().id(), created.id(), staleUpdate
        ));

        assertThat(thrown).isNotNull();
        assertThat(hasOptimisticLockCause(thrown)).isTrue();
        assertThat(lessonMaterialService.getLessonMaterial(
            fixture.principal(), fixture.topic().id(), created.id()
        )).satisfies(persisted -> {
            assertThat(persisted.title()).isEqualTo("Первая версия");
            assertThat(persisted.version()).isEqualTo(updated.version());
        });
    }

    private LessonMaterialResult createTextMaterial(ContentFixture fixture, String title, int position) {
        return createMaterial(fixture, LessonMaterialType.TEXT, title, "Содержимое", null, position);
    }

    private LessonMaterialResult createMaterial(
        ContentFixture fixture,
        LessonMaterialType type,
        String title,
        String content,
        String externalUrl,
        int position
    ) {
        return lessonMaterialService.createLessonMaterial(
            fixture.principal(),
            createCommand(fixture.topic().id(), type, title, content, externalUrl, position)
        );
    }

    private CreateLessonMaterialCommand createCommand(
        UUID topicId,
        LessonMaterialType type,
        String title,
        String content,
        String externalUrl,
        int position
    ) {
        return new CreateLessonMaterialCommand(
            topicId, type, title, content, null, externalUrl, position
        );
    }

    private UpdateLessonMaterialCommand updateTextCommand(
        LessonMaterialResult source,
        String title
    ) {
        return new UpdateLessonMaterialCommand(
            LessonMaterialType.TEXT,
            title,
            source.content(),
            null,
            null,
            source.position(),
            source.version()
        );
    }

    private ContentFixture createFixture(String email) {
        UserEntity user = new UserEntity(UUID.randomUUID(), email, "password-hash", UserStatus.ACTIVE);
        user.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(user);
        TeacherEntity teacher = teacherRepository.saveAndFlush(new TeacherEntity(
            UUID.randomUUID(), user, "Teacher"
        ));
        SubjectEntity subject = subjectRepository.saveAndFlush(new SubjectEntity(
            UUID.randomUUID(), teacher.id(), null, "Предмет " + UUID.randomUUID(),
            null, SubjectStatus.ACTIVE
        ));
        LearningProgramEntity learningProgram = learningProgramRepository.saveAndFlush(new LearningProgramEntity(
            UUID.randomUUID(), teacher.id(), subject.id(), "Программа", null,
            LearningProgramStatus.DRAFT
        ));
        ModuleEntity module = moduleRepository.saveAndFlush(new ModuleEntity(
            UUID.randomUUID(), learningProgram.getId(), "Модуль", null, 0
        ));
        TopicEntity topic = topicRepository.saveAndFlush(new TopicEntity(
            UUID.randomUUID(), module.id(), "Тема", null, 0, TopicStatus.DRAFT
        ));
        AuthenticatedUser principal = new AuthenticatedUser(
            user.id(),
            email,
            "",
            true,
            List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );
        return new ContentFixture(principal, teacher, topic);
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

    private record ContentFixture(
        AuthenticatedUser principal,
        TeacherEntity teacher,
        TopicEntity topic
    ) {
    }
}
