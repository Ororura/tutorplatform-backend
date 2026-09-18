package com.tutorplatform.user.infrastructure.persistence;

import com.tutorplatform.test.PostgresIntegrationTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.tutorplatform.user.domain.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaUserRepository.class, JpaTeacherRepository.class})
class IdentityRepositoryIntegrationTest extends PostgresIntegrationTest {

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_identity_repository", "008");
    }

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void flywayMigratesCleanPostgresAndRolesArePersisted() {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity(userId, "teacher@example.com", "password-hash", UserStatus.ACTIVE);
        user.addRole(UserRole.TEACHER);
        user.addRole(UserRole.STUDENT);

        userRepository.saveAndFlush(user);
        entityManager.clear();

        UserEntity persisted = userRepository.findById(userId).orElseThrow();
        assertThat(persisted.roles()).containsExactlyInAnyOrder(UserRole.TEACHER, UserRole.STUDENT);
        assertThat(persisted.createdAt()).isNotNull();
        assertThat(persisted.updatedAt()).isNotNull();
    }

    @Test
    void emailIsUniqueCaseInsensitively() {
        userRepository.saveAndFlush(new UserEntity(
            UUID.randomUUID(),
            "Teacher@Example.com",
            "password-hash",
            UserStatus.ACTIVE
        ));

        UserEntity duplicate = new UserEntity(
            UUID.randomUUID(),
            "teacher@example.com",
            "other-password-hash",
            UserStatus.ACTIVE
        );

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void teacherIsLinkedToUserAndCanBeFoundByUserId() {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity(userId, "linked@example.com", "password-hash", UserStatus.ACTIVE);
        user.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(user);

        UUID teacherId = UUID.randomUUID();
        teacherRepository.saveAndFlush(new TeacherEntity(teacherId, user, "Егор"));
        entityManager.clear();

        TeacherEntity persisted = teacherRepository.findByUserId(userId).orElseThrow();
        assertThat(persisted.id()).isEqualTo(teacherId);
        assertThat(persisted.userId()).isEqualTo(userId);
        assertThat(persisted.displayName()).isEqualTo("Егор");
    }

    @Test
    void emailLookupUsesCitextEquality() {
        UUID userId = UUID.randomUUID();
        userRepository.saveAndFlush(new UserEntity(
            userId,
            "Lookup@Example.com",
            "password-hash",
            UserStatus.ACTIVE
        ));
        entityManager.clear();

        assertThat(userRepository.findByEmail("lookup@example.com"))
            .map(UserEntity::id)
            .contains(userId);
        assertThat(userRepository.existsByEmail("LOOKUP@example.com")).isTrue();
    }
}
