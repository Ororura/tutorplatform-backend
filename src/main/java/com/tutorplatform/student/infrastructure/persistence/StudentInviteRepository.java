package com.tutorplatform.student.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentInviteRepository
    extends JpaRepository<StudentInviteEntity, UUID> {

    Optional<StudentInviteEntity> findByTokenHash(String tokenHash);

    @Query("""
        select invite.student.id
        from StudentInviteEntity invite
        where invite.tokenHash = :tokenHash
        """)
    Optional<UUID> findStudentIdByTokenHash(
        @Param("tokenHash") String tokenHash
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StudentInviteEntity> findLockedByTokenHash(
        String tokenHash
    );

    List<StudentInviteEntity>
    findAllByStudent_IdOrderByCreatedAtDesc(UUID studentId);

    Optional<StudentInviteEntity> findByIdAndStudent_Id(
        UUID id,
        UUID studentId
    );

    @Modifying
    @Query("""
        update StudentInviteEntity invite
        set invite.revokedAt = :revokedAt
        where invite.student.id = :studentId
          and invite.acceptedAt is null
          and invite.revokedAt is null
          and invite.expiresAt > :revokedAt
        """)
    int revokeActiveInvites(
        @Param("studentId") UUID studentId,
        @Param("revokedAt") Instant revokedAt
    );
}
