package com.tutorplatform.assessment.infrastructure.persistence;

import com.tutorplatform.assessment.domain.TeacherAssessmentEntity;
import com.tutorplatform.assessment.domain.TeacherAssessmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaTeacherAssessmentRepository.class)
class AssessmentPersistenceIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private TeacherAssessmentRepository assessmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.target", () -> "007");
    }

    @Test
    void teacherAssessmentAndLessonSessionIdAreSaved() {
        UUID lessonSessionId = createLessonSession();
        UUID assessmentId = UUID.randomUUID();

        TeacherAssessmentEntity saved = assessmentRepository.saveAndFlush(new TeacherAssessmentEntity(
            assessmentId, lessonSessionId, 3, 4, 5, 2, "Уверенный прогресс"
        ));

        assertThat(saved.getId()).isEqualTo(assessmentId);
        assertThat(saved.getLessonSessionId()).isEqualTo(lessonSessionId);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(assessmentRepository.findById(assessmentId))
            .get()
            .extracting(TeacherAssessmentEntity::getLessonSessionId)
            .isEqualTo(lessonSessionId);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5})
    void understandingScoreBoundariesAreSaved(int score) {
        TeacherAssessmentEntity saved = saveAssessment(createLessonSession(), score, null);

        assertThat(saved.getUnderstandingScore()).isEqualTo(score);
    }

    @ParameterizedTest
    @MethodSource("invalidScores")
    void databaseRejectsOutOfRangeScoreForEveryCriterion(String column, int score) {
        UUID lessonSessionId = createLessonSession();

        assertThatThrownBy(() -> jdbcTemplate.update(
            "insert into teacher_assessments (id, lesson_session_id, " + column + ") values (?, ?, ?)",
            UUID.randomUUID(), lessonSessionId, score
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nullScoresAndNullPublicCommentAreAllowed() {
        TeacherAssessmentEntity saved = saveAssessment(createLessonSession(), null, null);

        assertThat(saved.getUnderstandingScore()).isNull();
        assertThat(saved.getIndependenceScore()).isNull();
        assertThat(saved.getPracticeScore()).isNull();
        assertThat(saved.getHomeworkScore()).isNull();
        assertThat(saved.getPublicComment()).isNull();
    }

    @Test
    void publicCommentIsSaved() {
        TeacherAssessmentEntity saved = saveAssessment(
            createLessonSession(), 4, "Хорошо объясняет ход решения"
        );

        assertThat(assessmentRepository.findById(saved.getId()))
            .get()
            .extracting(TeacherAssessmentEntity::getPublicComment)
            .isEqualTo("Хорошо объясняет ход решения");
    }

    @Test
    void unknownLessonSessionForeignKeyIsRejected() {
        assertThatThrownBy(() -> jdbcTemplate.update(
            "insert into teacher_assessments (id, lesson_session_id) values (?, ?)",
            UUID.randomUUID(), UUID.randomUUID()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateLessonSessionIdIsRejected() {
        UUID lessonSessionId = createLessonSession();
        saveAssessment(lessonSessionId, 3, null);

        assertThatThrownBy(() -> jdbcTemplate.update(
            "insert into teacher_assessments (id, lesson_session_id) values (?, ?)",
            UUID.randomUUID(), lessonSessionId
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void assessmentIsFoundByLessonSessionId() {
        UUID lessonSessionId = createLessonSession();
        TeacherAssessmentEntity saved = saveAssessment(lessonSessionId, 3, null);

        assertThat(assessmentRepository.findByLessonSessionId(lessonSessionId))
            .get()
            .extracting(TeacherAssessmentEntity::getId)
            .isEqualTo(saved.getId());
    }

    @Test
    void scoreAndCommentAreUpdatedWithoutChangingCreatedAt() {
        TeacherAssessmentEntity assessment = saveAssessment(
            createLessonSession(), 2, "Первый комментарий"
        );
        Instant originalCreatedAt = assessment.getCreatedAt();
        Instant originalUpdatedAt = assessment.getUpdatedAt();

        assessment.update(5, 4, 3, 2, "Обновлённый комментарий");
        TeacherAssessmentEntity updated = assessmentRepository.saveAndFlush(assessment);

        assertThat(updated.getUnderstandingScore()).isEqualTo(5);
        assertThat(updated.getPublicComment()).isEqualTo("Обновлённый комментарий");
        assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    private static Stream<Arguments> invalidScores() {
        return Stream.of("understanding_score", "independence_score", "practice_score", "homework_score")
            .flatMap(column -> Stream.of(0, 6).map(score -> Arguments.of(column, score)));
    }

    private TeacherAssessmentEntity saveAssessment(
        UUID lessonSessionId,
        Integer understandingScore,
        String publicComment
    ) {
        return assessmentRepository.saveAndFlush(new TeacherAssessmentEntity(
            UUID.randomUUID(), lessonSessionId, understandingScore, null, null, null, publicComment
        ));
    }

    private UUID createLessonSession() {
        UUID userId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID learningProgramId = UUID.randomUUID();
        UUID studentProgramId = UUID.randomUUID();
        UUID lessonSessionId = UUID.randomUUID();
        Instant now = Instant.now();

        jdbcTemplate.update(
            "insert into users (id, email, password_hash, status) values (?, ?, ?, 'ACTIVE')",
            userId, "assessment-" + userId + "@example.com", "password-hash"
        );
        jdbcTemplate.update(
            "insert into teachers (id, user_id, display_name) values (?, ?, ?)",
            teacherId, userId, "Teacher"
        );
        jdbcTemplate.update(
            "insert into students (id, first_name, status) values (?, ?, 'ACTIVE')",
            studentId, "Ученик"
        );
        jdbcTemplate.update(
            "insert into subjects (id, owner_teacher_id, name, status) values (?, ?, ?, 'ACTIVE')",
            subjectId, teacherId, "Предмет " + subjectId
        );
        jdbcTemplate.update(
            "insert into learning_programs (id, teacher_id, subject_id, title, status) "
                + "values (?, ?, ?, ?, 'DRAFT')",
            learningProgramId, teacherId, subjectId, "Программа"
        );
        jdbcTemplate.update(
            "insert into student_programs (id, student_id, learning_program_id, "
                + "assigned_by_teacher_id, status, report_interval_minutes, started_at) "
                + "values (?, ?, ?, ?, 'ACTIVE', 480, ?)",
            studentProgramId, studentId, learningProgramId, teacherId, Timestamp.from(now)
        );
        jdbcTemplate.update(
            "insert into lesson_sessions (id, student_program_id, teacher_id, started_at, "
                + "duration_minutes, attendance_status) values (?, ?, ?, ?, 60, 'ATTENDED')",
            lessonSessionId, studentProgramId, teacherId, Timestamp.from(now)
        );
        return lessonSessionId;
    }
}
