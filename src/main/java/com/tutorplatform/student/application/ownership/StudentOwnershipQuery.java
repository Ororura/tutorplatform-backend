package com.tutorplatform.student.application;

import java.util.Optional;
import java.util.UUID;

public interface StudentOwnershipQuery {

    Optional<UUID> findTeacherIdByUserId(UUID userId);

    Optional<UUID> findStudentIdByUserId(UUID userId);

    boolean isActivePrimaryOwner(UUID teacherId, UUID studentId);
}
