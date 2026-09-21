package com.tutorplatform.assessment.infrastructure.persistence;

import com.tutorplatform.assessment.domain.TeacherAssessmentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "teacher_assessments")
public class TeacherAssessmentDatabaseModel {

    @Id private UUID id;

    @Column(name = "lesson_session_id", nullable = false, unique = true, updatable = false)
    private UUID lessonSessionId;

    @Column(name = "understanding_score")
    private Short understandingScore;

    @Column(name = "independence_score")
    private Short independenceScore;

    @Column(name = "practice_score")
    private Short practiceScore;

    @Column(name = "homework_score")
    private Short homeworkScore;

    @Column(name = "public_comment")
    private String publicComment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TeacherAssessmentDatabaseModel() {}

    TeacherAssessmentDatabaseModel(TeacherAssessmentEntity assessment) {
        id = Objects.requireNonNull(assessment.getId());
        lessonSessionId = Objects.requireNonNull(assessment.getLessonSessionId());
        updateFrom(assessment);
    }

    void updateFrom(TeacherAssessmentEntity assessment) {
        if (!Objects.requireNonNull(assessment.getLessonSessionId()).equals(lessonSessionId)) {
            throw new IllegalArgumentException("Assessment lesson session cannot be changed");
        }
        understandingScore = toDatabaseScore(assessment.getUnderstandingScore());
        independenceScore = toDatabaseScore(assessment.getIndependenceScore());
        practiceScore = toDatabaseScore(assessment.getPracticeScore());
        homeworkScore = toDatabaseScore(assessment.getHomeworkScore());
        publicComment = assessment.getPublicComment();
    }

    TeacherAssessmentEntity toEntity() {
        return new TeacherAssessmentEntity(
                id,
                lessonSessionId,
                toDomainScore(understandingScore),
                toDomainScore(independenceScore),
                toDomainScore(practiceScore),
                toDomainScore(homeworkScore),
                publicComment,
                createdAt,
                updatedAt);
    }

    private static Short toDatabaseScore(Integer score) {
        return score == null ? null : score.shortValue();
    }

    private static Integer toDomainScore(Short score) {
        return score == null ? null : score.intValue();
    }
}
