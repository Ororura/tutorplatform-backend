package com.tutorplatform.student.infrastructure.persistence;

import com.tutorplatform.student.domain.StudentEntity;
import com.tutorplatform.student.domain.StudentRepository;
import com.tutorplatform.student.domain.StudentStatus;
import com.tutorplatform.user.domain.UserRole;
import com.tutorplatform.user.domain.UserStatus;
import com.tutorplatform.user.infrastructure.persistence.JpaTeacherRepository;
import com.tutorplatform.user.infrastructure.persistence.JpaUserRepository;
import com.tutorplatform.user.domain.TeacherEntity;
import com.tutorplatform.user.domain.TeacherRepository;
import com.tutorplatform.user.domain.UserEntity;
import com.tutorplatform.user.domain.UserRepository;
import jakarta.persistence.EntityManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaStudentRepository.class, JpaUserRepository.class, JpaTeacherRepository.class})
class StudentRepositoryIntegrationTest {

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
    private StudentRepository studentRepository;

    @Autowired
    private TeacherStudentLinkRepository teacherStudentLinkRepository;

    @Autowired
    private StudentInviteRepository studentInviteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void studentCanBePersistedWithoutUserAccount() {
        UUID studentId = UUID.randomUUID();

        studentRepository.saveAndFlush(new StudentEntity(
                studentId,
                "Андрей",
                null,
                StudentStatus.ACTIVE
        ));
        entityManager.clear();

        StudentEntity persisted = studentRepository.findById(studentId).orElseThrow();
        assertThat(persisted.getUserId()).isNull();
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getUpdatedAt()).isNotNull();
    }

    @Test
    void secondActivePrimaryTeacherIsRejected() {
        TeacherEntity firstTeacher = createTeacher("first-teacher@example.com");
        TeacherEntity secondTeacher = createTeacher("second-teacher@example.com");
        StudentEntity student = studentRepository.saveAndFlush(new StudentEntity(
                UUID.randomUUID(),
                "Андрей",
                "Иванов",
                StudentStatus.ACTIVE
        ));

        teacherStudentLinkRepository.saveAndFlush(new TeacherStudentLinkEntity(firstTeacher, student));

        assertThatThrownBy(() -> teacherStudentLinkRepository.saveAndFlush(
                new TeacherStudentLinkEntity(secondTeacher, student)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void inviteTokenHashMustBeUnique() {
        TeacherEntity teacher = createTeacher("invite-teacher@example.com");
        StudentEntity student = studentRepository.saveAndFlush(new StudentEntity(
                UUID.randomUUID(),
                "Мария",
                null,
                StudentStatus.ACTIVE
        ));
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        studentInviteRepository.saveAndFlush(new StudentInviteEntity(
                UUID.randomUUID(),
                student,
                teacher,
                "first@example.com",
                "same-token-hash",
                expiresAt
        ));

        assertThatThrownBy(() -> studentInviteRepository.saveAndFlush(new StudentInviteEntity(
                UUID.randomUUID(),
                student,
                teacher,
                "second@example.com",
                "same-token-hash",
                expiresAt
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cleanPostgresIsMigratedFromV001ThroughV003() {
        assertThat(Arrays.stream(flyway.info().applied())
                .map(migration -> migration.getVersion().toString()))
                .containsExactly("001", "002", "003");

        assertThat(jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.tables
                        where table_schema = 'public'
                          and table_name in ('students', 'teacher_student_links', 'student_invites')
                        """,
                Integer.class
        )).isEqualTo(3);
    }

    private TeacherEntity createTeacher(String email) {
        UserEntity user = new UserEntity(UUID.randomUUID(), email, "password-hash", UserStatus.ACTIVE);
        user.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(user);

        return teacherRepository.saveAndFlush(new TeacherEntity(
                UUID.randomUUID(),
                user,
                "Teacher"
        ));
    }
}
