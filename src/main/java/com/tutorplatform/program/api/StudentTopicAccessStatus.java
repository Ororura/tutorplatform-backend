package com.tutorplatform.program.api;

import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressStatus;

public enum StudentTopicAccessStatus {
    LOCKED,
    AVAILABLE;

    public StudentTopicProgressStatus toProgressStatus() {
        return StudentTopicProgressStatus.valueOf(name());
    }
}
