package com.tutorplatform.assessment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class TeacherAssessmentEntityTest {

    @ParameterizedTest
    @EnumSource(ScoreField.class)
    void nullScoreIsAllowedForEveryCriterion(ScoreField field) {
        TeacherAssessmentEntity assessment = field.assessmentWith(null);

        assertThat(field.scoreFrom(assessment)).isNull();
    }

    @ParameterizedTest
    @MethodSource("validBoundaryScores")
    void boundaryScoresAreAllowedForEveryCriterion(ScoreField field, int score) {
        TeacherAssessmentEntity assessment = field.assessmentWith(score);

        assertThat(field.scoreFrom(assessment)).isEqualTo(score);
    }

    @ParameterizedTest
    @MethodSource("invalidBoundaryScores")
    void outOfRangeScoresAreRejectedForEveryCriterion(ScoreField field, int score) {
        assertThatThrownBy(() -> field.assessmentWith(score))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1 and 5");
    }

    private static Stream<Arguments> validBoundaryScores() {
        return scoreCases(1, 5);
    }

    private static Stream<Arguments> invalidBoundaryScores() {
        return scoreCases(0, 6);
    }

    private static Stream<Arguments> scoreCases(int lower, int upper) {
        return Stream.of(ScoreField.values())
                .flatMap(field -> Stream.of(lower, upper).map(score -> Arguments.of(field, score)));
    }

    private enum ScoreField {
        UNDERSTANDING {
            @Override
            TeacherAssessmentEntity assessmentWith(Integer score) {
                return assessment(score, null, null, null);
            }

            @Override
            Integer scoreFrom(TeacherAssessmentEntity assessment) {
                return assessment.getUnderstandingScore();
            }
        },
        INDEPENDENCE {
            @Override
            TeacherAssessmentEntity assessmentWith(Integer score) {
                return assessment(null, score, null, null);
            }

            @Override
            Integer scoreFrom(TeacherAssessmentEntity assessment) {
                return assessment.getIndependenceScore();
            }
        },
        PRACTICE {
            @Override
            TeacherAssessmentEntity assessmentWith(Integer score) {
                return assessment(null, null, score, null);
            }

            @Override
            Integer scoreFrom(TeacherAssessmentEntity assessment) {
                return assessment.getPracticeScore();
            }
        },
        HOMEWORK {
            @Override
            TeacherAssessmentEntity assessmentWith(Integer score) {
                return assessment(null, null, null, score);
            }

            @Override
            Integer scoreFrom(TeacherAssessmentEntity assessment) {
                return assessment.getHomeworkScore();
            }
        };

        abstract TeacherAssessmentEntity assessmentWith(Integer score);

        abstract Integer scoreFrom(TeacherAssessmentEntity assessment);

        TeacherAssessmentEntity assessment(
                Integer understandingScore,
                Integer independenceScore,
                Integer practiceScore,
                Integer homeworkScore) {
            return new TeacherAssessmentEntity(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    understandingScore,
                    independenceScore,
                    practiceScore,
                    homeworkScore,
                    null);
        }
    }
}
