package com.tutorplatform.subject.domain;

import java.util.Optional;
import java.util.UUID;

public interface SubjectRepository {
    SubjectEntity saveAndFlush(SubjectEntity subject);

    Optional<SubjectEntity> findById(UUID subjectId);
}
