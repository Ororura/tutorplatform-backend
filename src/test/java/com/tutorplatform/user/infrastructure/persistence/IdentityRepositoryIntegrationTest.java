package com.tutorplatform.user.infrastructure.persistence;

import com.tutorplatform.user.domain.TeacherEntity;
import com.tutorplatform.user.domain.TeacherRepository;
import com.tutorplatform.user.domain.UserEntity;
import com.tutorplatform.user.domain.UserRepository;
import com.tutorplatform.user.domain.UserRole;
import com.tutorplatform.user.domain.UserStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaUserRepository.class, JpaTeacherRepository.class})
class IdentityRepositoryIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.target", () -> "003");
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
        assertThat(persisted.getRoles()).containsExactlyInAnyOrder(UserRole.TEACHER, UserRole.STUDENT);
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getUpdatedAt()).isNotNull();
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
        assertThat(persisted.getId()).isEqualTo(teacherId);
        assertThat(persisted.getUserId()).isEqualTo(userId);
        assertThat(persisted.getDisplayName()).isEqualTo("Егор");
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
                .map(UserEntity::getId)
                .contains(userId);
        assertThat(userRepository.existsByEmail("LOOKUP@example.com")).isTrue();
    }
}
