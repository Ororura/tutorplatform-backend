package com.tutorplatform.assessment.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class TeacherAssessmentEntity {

    private final UUID id;
    private final UUID lessonSessionId;
    private final Instant createdAt;
    private final Instant updatedAt;
    private Integer understandingScore;
    private Integer independenceScore;
    private Integer practiceScore;
    private Integer homeworkScore;
    private String publicComment;

    public TeacherAssessmentEntity(
        UUID id,
        UUID lessonSessionId,
        Integer understandingScore,
        Integer independenceScore,
        Integer practiceScore,
        Integer homeworkScore,
        String publicComment
    ) {
        this(id, lessonSessionId, understandingScore, independenceScore, practiceScore,
            homeworkScore, publicComment, null, null);
    }

    public TeacherAssessmentEntity(
        UUID id,
        UUID lessonSessionId,
        Integer understandingScore,
        Integer independenceScore,
        Integer practiceScore,
        Integer homeworkScore,
        String publicComment,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.lessonSessionId = Objects.requireNonNull(lessonSessionId);
        applyChanges(understandingScore, independenceScore, practiceScore, homeworkScore, publicComment);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void update(
        Integer understandingScore,
        Integer independenceScore,
        Integer practiceScore,
        Integer homeworkScore,
        String publicComment
    ) {
        applyChanges(understandingScore, independenceScore, practiceScore, homeworkScore, publicComment);
    }

    private void applyChanges(
        Integer understandingScore,
        Integer independenceScore,
        Integer practiceScore,
        Integer homeworkScore,
        String publicComment
    ) {
        validateScore("understandingScore", understandingScore);
        validateScore("independenceScore", independenceScore);
        validateScore("practiceScore", practiceScore);
        validateScore("homeworkScore", homeworkScore);
        this.understandingScore = understandingScore;
        this.independenceScore = independenceScore;
        this.practiceScore = practiceScore;
        this.homeworkScore = homeworkScore;
        this.publicComment = publicComment;
    }

    private static void validateScore(String fieldName, Integer score) {
        if (score != null && (score < 1 || score > 5)) {
            throw new IllegalArgumentException(fieldName + " must be between 1 and 5");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getLessonSessionId() {
        return lessonSessionId;
    }

    public Integer getUnderstandingScore() {
        return understandingScore;
    }

    public Integer getIndependenceScore() {
        return independenceScore;
    }

    public Integer getPracticeScore() {
        return practiceScore;
    }

    public Integer getHomeworkScore() {
        return homeworkScore;
    }

    public String getPublicComment() {
        return publicComment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
