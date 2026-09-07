package com.tutorplatform.program.domain.studentprogram;

import java.util.Optional;
import java.util.UUID;

public interface StudentTopicProgressRepository {
    StudentTopicProgressEntity saveAndFlush(StudentTopicProgressEntity progress);
    Optional<StudentTopicProgressEntity> findById(UUID studentProgramId, UUID topicId);
}
