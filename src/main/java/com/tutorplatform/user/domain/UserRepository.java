package com.tutorplatform.user.domain;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    UserEntity saveAndFlush(UserEntity user);
    Optional<UserEntity> findById(UUID id);
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);
    long count();
    void deleteAll();
}
