package com.tutorplatform.platform.infrastructure.persistence;

import com.tutorplatform.platform.domain.TeacherRegistrationInvite;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TeacherRegistrationInviteRepository {

    private static final String SELECT_INVITE = """
        SELECT
            id,
            created_by_admin_id,
            email,
            token_hash,
            expires_at,
            accepted_at,
            revoked_at,
            created_at
        FROM teacher_registration_invites
        """;

    private final JdbcClient jdbcClient;

    public TeacherRegistrationInviteRepository(
        JdbcClient jdbcClient
    ) {
        this.jdbcClient = jdbcClient;
    }

    public void create(
        UUID id,
        UUID adminId,
        String email,
        String tokenHash,
        Instant expiresAt
    ) {
        int inserted = jdbcClient.sql("""
                INSERT INTO teacher_registration_invites (
                    id,
                    created_by_admin_id,
                    email,
                    token_hash,
                    expires_at
                )
                VALUES (
                    :id,
                    :adminId,
                    :email,
                    :tokenHash,
                    :expiresAt
                )
                """)
            .param("id", id)
            .param("adminId", adminId)
            .param("email", email)
            .param("tokenHash", tokenHash)
            .param("expiresAt", expiresAt.atOffset(ZoneOffset.UTC))
            .update();

        if (inserted != 1) {
            throw new IllegalStateException(
                "Failed to create teacher registration invitation"
            );
        }
    }

    public Optional<TeacherRegistrationInvite> findByTokenHash(
        String tokenHash
    ) {
        return jdbcClient.sql(
                SELECT_INVITE + """
                WHERE token_hash = :tokenHash
                """
            )
            .param("tokenHash", tokenHash)
            .query(TeacherRegistrationInviteRepository::mapRow)
            .optional();
    }

    public Optional<TeacherRegistrationInvite> findByTokenHashForUpdate(
        String tokenHash
    ) {
        return jdbcClient.sql(
                SELECT_INVITE + """
                WHERE token_hash = :tokenHash
                FOR UPDATE
                """
            )
            .param("tokenHash", tokenHash)
            .query(TeacherRegistrationInviteRepository::mapRow)
            .optional();
    }

    public List<TeacherRegistrationInvite> findAllByAdminId(
        UUID adminId
    ) {
        return jdbcClient.sql(
                SELECT_INVITE + """
                WHERE created_by_admin_id = :adminId
                ORDER BY created_at DESC, id DESC
                """
            )
            .param("adminId", adminId)
            .query(TeacherRegistrationInviteRepository::mapRow)
            .list();
    }


    public Optional<TeacherRegistrationInvite> findByIdAndAdminId(
        UUID invitationId,
        UUID adminId
    ) {
        return jdbcClient.sql(
                SELECT_INVITE + """
                WHERE id = :invitationId
                  AND created_by_admin_id = :adminId
                """
            )
            .param("invitationId", invitationId)
            .param("adminId", adminId)
            .query(TeacherRegistrationInviteRepository::mapRow)
            .optional();
    }

    public boolean existsRegisteredUserByEmail(String email) {
        return jdbcClient.sql("""
                SELECT EXISTS (
                    SELECT 1
                    FROM users
                    WHERE email = :email
                )
                """)
            .param("email", email)
            .query(Boolean.class)
            .single();
    }

    public int revokeActive(
        UUID invitationId,
        UUID adminId
    ) {
        return jdbcClient.sql("""
                UPDATE teacher_registration_invites
                SET revoked_at = statement_timestamp()
                WHERE id = :invitationId
                  AND created_by_admin_id = :adminId
                  AND accepted_at IS NULL
                  AND revoked_at IS NULL
                  AND expires_at > statement_timestamp()
                """)
            .param("invitationId", invitationId)
            .param("adminId", adminId)
            .update();
    }

    public int markAccepted(
        UUID invitationId
    ) {
        return jdbcClient.sql("""
                UPDATE teacher_registration_invites
                SET accepted_at = statement_timestamp()
                WHERE id = :invitationId
                  AND accepted_at IS NULL
                  AND revoked_at IS NULL
                  AND expires_at > statement_timestamp()
                """)
            .param("invitationId", invitationId)
            .update();
    }

    private static TeacherRegistrationInvite mapRow(
        ResultSet rs,
        int rowNum
    ) throws SQLException {
        return new TeacherRegistrationInvite(
            rs.getObject("id", UUID.class),
            rs.getObject("created_by_admin_id", UUID.class),
            rs.getString("email"),
            rs.getString("token_hash"),
            rs.getTimestamp("expires_at").toInstant(),
            nullableInstant(rs.getTimestamp("accepted_at")),
            nullableInstant(rs.getTimestamp("revoked_at")),
            rs.getTimestamp("created_at").toInstant()
        );
    }

    private static Instant nullableInstant(
        Timestamp timestamp
    ) {
        return timestamp == null
            ? null
            : timestamp.toInstant();
    }
}
