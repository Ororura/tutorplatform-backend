package com.tutorplatform.program.domain.studentprogram;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentTopicProgressRepository {
    StudentTopicProgressEntity saveAndFlush(StudentTopicProgressEntity progress);

    Optional<StudentTopicProgressEntity> findById(UUID studentProgramId, UUID topicId);

    List<StudentTopicProgressEntity> findAllForUpdate(UUID studentProgramId, List<UUID> topicIds);
}
