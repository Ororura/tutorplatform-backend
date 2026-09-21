package com.tutorplatform.subject.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubjectRepository {
    SubjectEntity saveAndFlush(SubjectEntity subject);

    Optional<SubjectEntity> findById(UUID subjectId);

    List<SubjectEntity> findAccessibleByTeacherAndStatus(UUID teacherId, SubjectStatus status);
}
