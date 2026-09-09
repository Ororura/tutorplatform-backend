package com.tutorplatform.user.domain;

import java.util.Optional;
import java.util.UUID;

public interface TeacherRepository {
    TeacherEntity save(TeacherEntity teacher);

    TeacherEntity saveAndFlush(TeacherEntity teacher);

    Optional<TeacherEntity> findByUserId(UUID userId);

    long count();

    void deleteAll();
}
