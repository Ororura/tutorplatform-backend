package com.tutorplatform.submission.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SubmissionReviewedEventTest {

    @Test
    void eventContainsOnlySafeIdentifiersAndResultingStatus() {
        assertThat(
                        Arrays.stream(SubmissionReviewedEvent.class.getRecordComponents())
                                .map(RecordComponent::getName))
                .containsExactly(
                        "submissionId",
                        "studentId",
                        "studentProgramId",
                        "taskId",
                        "homeworkItemId",
                        "resultingStatus");
    }
}
