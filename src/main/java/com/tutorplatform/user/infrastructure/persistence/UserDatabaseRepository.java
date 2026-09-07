package com.tutorplatform.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface UserDatabaseRepository extends JpaRepository<UserDatabaseModel, UUID> {
    @Query(value = "SELECT * FROM users WHERE email = CAST(:email AS citext)", nativeQuery = true)
    Optional<UserDatabaseModel> findByEmail(@Param("email") String email);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM users WHERE email = CAST(:email AS citext))", nativeQuery = true)
    boolean existsByEmail(@Param("email") String email);
}
