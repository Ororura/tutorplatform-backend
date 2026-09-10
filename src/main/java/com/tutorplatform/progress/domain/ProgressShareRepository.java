package com.tutorplatform.progress.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgressShareRepository {

    ProgressShare saveAndFlush(ProgressShare share);

    Optional<ProgressShare> findByTokenHash(String tokenHash);

    List<ProgressShare> findAllOwnedBy(UUID teacherId, UUID studentId, UUID studentProgramId);

    Optional<ProgressShare> findOwnedById(UUID shareId, UUID teacherId, UUID studentId);
}
