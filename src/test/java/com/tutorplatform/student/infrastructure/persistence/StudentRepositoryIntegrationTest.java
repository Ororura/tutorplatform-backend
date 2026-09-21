package com.tutorplatform.student.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tutorplatform.student.domain.StudentEntity;
import com.tutorplatform.student.domain.StudentRepository;
import com.tutorplatform.student.domain.StudentStatus;
import com.tutorplatform.test.PostgresIntegrationTest;
import com.tutorplatform.user.domain.*;
import com.tutorplatform.user.infrastructure.persistence.JpaTeacherRepository;
import com.tutorplatform.user.infrastructure.persistence.JpaUserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaStudentRepository.class, JpaUserRepository.class, JpaTeacherRepository.class})
class StudentRepositoryIntegrationTest extends PostgresIntegrationTest {

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_student_repository", "008");
    }

    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherStudentLinkRepository teacherStudentLinkRepository;
    @Autowired private StudentInviteRepository studentInviteRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private Flyway flyway;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void studentCanBePersistedWithoutUserAccount() {
        UUID studentId = UUID.randomUUID();

        studentRepository.saveAndFlush(
                new StudentEntity(studentId, "Андрей", null, StudentStatus.ACTIVE));
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
        StudentEntity student =
                studentRepository.saveAndFlush(
                        new StudentEntity(
                                UUID.randomUUID(), "Андрей", "Иванов", StudentStatus.ACTIVE));

        teacherStudentLinkRepository.saveAndFlush(
                new TeacherStudentLinkEntity(firstTeacher, student));

        assertThatThrownBy(
                        () ->
                                teacherStudentLinkRepository.saveAndFlush(
                                        new TeacherStudentLinkEntity(secondTeacher, student)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void inviteTokenHashMustBeUnique() {
        TeacherEntity teacher = createTeacher("invite-teacher@example.com");
        StudentEntity student =
                studentRepository.saveAndFlush(
                        new StudentEntity(UUID.randomUUID(), "Мария", null, StudentStatus.ACTIVE));
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        studentInviteRepository.saveAndFlush(
                new StudentInviteEntity(
                        UUID.randomUUID(),
                        student,
                        teacher,
                        "first@example.com",
                        "same-token-hash",
                        expiresAt));

        assertThatThrownBy(
                        () ->
                                studentInviteRepository.saveAndFlush(
                                        new StudentInviteEntity(
                                                UUID.randomUUID(),
                                                student,
                                                teacher,
                                                "second@example.com",
                                                "same-token-hash",
                                                expiresAt)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cleanPostgresIsMigratedThroughV006() {
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
                  and table_name in ('students', 'teacher_student_links', 'student_invites')
                """,
                                Integer.class))
                .isEqualTo(3);
    }

    private TeacherEntity createTeacher(String email) {
        UserEntity user =
                new UserEntity(UUID.randomUUID(), email, "password-hash", UserStatus.ACTIVE);
        user.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(user);

        return teacherRepository.saveAndFlush(
                new TeacherEntity(UUID.randomUUID(), user, "Teacher"));
    }
}
