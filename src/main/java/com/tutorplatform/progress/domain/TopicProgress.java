package com.tutorplatform.progress.domain;

import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressStatus;

import java.util.Objects;
import java.util.UUID;

public record TopicProgress(UUID topicId, String title, StudentTopicProgressStatus status) {
    public TopicProgress {
        Objects.requireNonNull(topicId);
        Objects.requireNonNull(title);
        Objects.requireNonNull(status);
    }
}
