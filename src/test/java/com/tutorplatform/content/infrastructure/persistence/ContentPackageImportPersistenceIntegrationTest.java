package com.tutorplatform.content.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tutorplatform.content.application.importpackage.ContentPackageImportRecord;
import com.tutorplatform.content.application.importpackage.ContentPackageImportRepository;
import com.tutorplatform.test.PostgresIntegrationTest;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JdbcContentPackageImportRepository.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ContentPackageImportPersistenceIntegrationTest extends PostgresIntegrationTest {

    private static final String DIGEST = "a".repeat(64);

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_content_package_import", null);
    }

    @Autowired private ContentPackageImportRepository repository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;

    private UUID teacherId;
    private UUID programId;

    @BeforeEach
    void setUp() {
        teacherId = teacher();
        programId = program(teacherId);
    }

    @Test
    void savesAndFindsSuccessfulResultIncludingDigestAndOrderedModuleIds() {
        UUID confirmationId = UUID.randomUUID();
        List<UUID> moduleIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        ContentPackageImportRecord saved =
                transaction(
                        () -> {
                            assertThat(
                                            repository.registerConfirmation(
                                                    teacherId, programId, confirmationId))
                                    .isEmpty();
                            return repository.saveSuccessfulImport(
                                    teacherId,
                                    programId,
                                    confirmationId,
                                    DIGEST,
                                    2,
                                    3,
                                    4,
                                    moduleIds);
                        });

        assertThat(saved.id()).isNotNull();
        assertThat(saved.createdAt()).isNotNull();
        assertThat(saved.packageDigest()).isEqualTo(DIGEST);
        assertThat(saved.createdModuleIds()).containsExactlyElementsOf(moduleIds);
        assertThat(repository.findByConfirmation(teacherId, programId, confirmationId))
                .contains(saved);
        assertThat(
                        transaction(
                                () ->
                                        repository.registerConfirmation(
                                                teacherId, programId, confirmationId)))
                .contains(saved);
    }

    @Test
    void confirmationIsScopedToTeacherAndProgram() {
        UUID confirmationId = UUID.randomUUID();
        UUID otherTeacherId = teacher();
        UUID otherProgramId = program(teacherId);
        UUID otherTeachersProgramId = program(otherTeacherId);
        transaction(
                () ->
                        repository.saveSuccessfulImport(
                                teacherId,
                                programId,
                                confirmationId,
                                DIGEST,
                                1,
                                1,
                                0,
                                List.of(UUID.randomUUID())));

        assertThat(repository.findByConfirmation(otherTeacherId, programId, confirmationId))
                .isEmpty();
        assertThat(repository.findByConfirmation(teacherId, otherProgramId, confirmationId))
                .isEmpty();
        assertThat(
                        transaction(
                                () ->
                                        repository.registerConfirmation(
                                                teacherId, otherProgramId, confirmationId)))
                .isEmpty();
        assertThat(
                        transaction(
                                () ->
                                        repository.registerConfirmation(
                                                otherTeacherId,
                                                otherTeachersProgramId,
                                                confirmationId)))
                .isEmpty();
        transaction(
                () ->
                        repository.saveSuccessfulImport(
                                teacherId,
                                otherProgramId,
                                confirmationId,
                                DIGEST,
                                1,
                                1,
                                0,
                                List.of(UUID.randomUUID())));
        transaction(
                () ->
                        repository.saveSuccessfulImport(
                                otherTeacherId,
                                otherTeachersProgramId,
                                confirmationId,
                                DIGEST,
                                1,
                                1,
                                0,
                                List.of(UUID.randomUUID())));
    }

    @Test
    void sameFileCanBeImportedWithAnotherConfirmation() {
        save(UUID.randomUUID());
        save(UUID.randomUUID());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM content_package_imports WHERE teacher_id = ? AND learning_program_id = ? AND package_digest = ?",
                                Integer.class,
                                teacherId,
                                programId,
                                DIGEST))
                .isEqualTo(2);
    }

    @Test
    void databaseRejectsDuplicateConfirmation() {
        UUID confirmationId = UUID.randomUUID();
        save(confirmationId);
        assertThatThrownBy(() -> save(confirmationId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rollbackLeavesNoSuccessfulConfirmation() {
        UUID confirmationId = UUID.randomUUID();
        assertThatThrownBy(
                        () ->
                                transaction(
                                        () -> {
                                            repository.registerConfirmation(
                                                    teacherId, programId, confirmationId);
                                            repository.saveSuccessfulImport(
                                                    teacherId,
                                                    programId,
                                                    confirmationId,
                                                    DIGEST,
                                                    1,
                                                    1,
                                                    0,
                                                    List.of(UUID.randomUUID()));
                                            throw new IllegalStateException(
                                                    "simulate failed import");
                                        }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(repository.findByConfirmation(teacherId, programId, confirmationId)).isEmpty();
    }

    @Test
    void concurrentConfirmationWaitsAndReplaysCommittedResult() throws Exception {
        UUID confirmationId = UUID.randomUUID();
        CountDownLatch firstRegistered = new CountDownLatch(1);
        CountDownLatch allowCommit = new CountDownLatch(1);
        try (var executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())) {
            var first =
                    executor.submit(
                            () ->
                                    transaction(
                                            () -> {
                                                assertThat(
                                                                repository.registerConfirmation(
                                                                        teacherId,
                                                                        programId,
                                                                        confirmationId))
                                                        .isEmpty();
                                                firstRegistered.countDown();
                                                await(allowCommit);
                                                return repository.saveSuccessfulImport(
                                                        teacherId,
                                                        programId,
                                                        confirmationId,
                                                        DIGEST,
                                                        1,
                                                        1,
                                                        0,
                                                        List.of(UUID.randomUUID()));
                                            }));
            assertThat(firstRegistered.await(5, TimeUnit.SECONDS)).isTrue();
            var second =
                    executor.submit(
                            () ->
                                    transaction(
                                            () ->
                                                    repository.registerConfirmation(
                                                            teacherId, programId, confirmationId)));
            allowCommit.countDown();
            ContentPackageImportRecord saved = first.get(5, TimeUnit.SECONDS);
            assertThat(second.get(5, TimeUnit.SECONDS)).contains(saved);
        } finally {
            allowCommit.countDown();
        }
    }

    private ContentPackageImportRecord save(UUID confirmationId) {
        return transaction(
                () ->
                        repository.saveSuccessfulImport(
                                teacherId,
                                programId,
                                confirmationId,
                                DIGEST,
                                1,
                                1,
                                0,
                                List.of(UUID.randomUUID())));
    }

    private <T> T transaction(java.util.function.Supplier<T> action) {
        return new TransactionTemplate(transactionManager).execute(status -> action.get());
    }

    private UUID teacher() {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, email) VALUES (?, ?)", userId, userId + "@example.com");
        jdbc.update(
                "INSERT INTO teachers (id, user_id, display_name) VALUES (?, ?, 'Teacher')",
                id,
                userId);
        return id;
    }

    private UUID program(UUID ownerId) {
        UUID subjectId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO subjects (id, owner_teacher_id, name) VALUES (?, ?, ?)",
                subjectId,
                ownerId,
                "Subject " + subjectId);
        jdbc.update(
                "INSERT INTO learning_programs (id, teacher_id, subject_id, title) VALUES (?, ?, ?, 'Program')",
                id,
                ownerId,
                subjectId);
        return id;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for concurrent import");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
