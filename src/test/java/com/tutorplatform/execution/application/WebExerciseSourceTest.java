package com.tutorplatform.execution.application;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class WebExerciseSourceTest {
    private static final String STARTER =
            """
        {"html":"<button>+</button>","css":"","javascript":"", "editableFiles":["javascript"],"reviewMode":"HYBRID"}
        """;

    @Test
    void acceptsOnlyEditableChangesAndReadsTeacherReviewMode() {
        assertThat(
                        WebExerciseSource.normalize(
                                STARTER,
                                """
            {"html":"<button>+</button>","css":"","javascript":"let n = 1;"}
            """))
                .contains("let n = 1;");
        assertThat(WebExerciseSource.reviewMode(STARTER))
                .isEqualTo(WebExerciseSource.ReviewMode.HYBRID);
    }

    @Test
    void rejectsLockedFileChangesAndStudentReviewPolicy() {
        assertThatThrownBy(
                        () ->
                                WebExerciseSource.normalize(
                                        STARTER,
                                        """
            {"html":"<button>hacked</button>","css":"","javascript":""}
            """))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                WebExerciseSource.normalize(
                                        STARTER,
                                        """
            {"html":"<button>+</button>","css":"","javascript":"","reviewMode":"AUTO"}
            """))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidOrEmptyManifests() {
        for (String input :
                new String[] {
                    "{}",
                    "[]",
                    "null",
                    "{",
                    STARTER.replace("HYBRID", "INVALID"),
                    STARTER.replace("[\"javascript\"]", "[]")
                }) {
            assertThatThrownBy(() -> WebExerciseSource.manifest(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
