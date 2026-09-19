package com.tutorplatform.platform.infrastructure.persistence;

import com.tutorplatform.test.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TeacherRegistrationInviteRepositoryIntegrationTest
    extends PostgresIntegrationTest {

    private static final String ADMIN_EMAIL =
        "registration-invite-admin@example.com";

    @DynamicPropertySource
    static void configurePostgres(
        DynamicPropertyRegistry registry
    ) {
        PostgresIntegrationTest.configurePostgres(
            registry,
            "test_teacher_registration_invites_repository",
            null
        );
    }

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private TeacherRegistrationInviteRepository repository;

    @BeforeEach
    void cleanDatabase() {
        jdbcClient.sql(
            "DELETE FROM teacher_registration_invites"
        ).update();

        jdbcClient.sql("""
                DELETE FROM users
                WHERE email = :email
                """)
            .param("email", ADMIN_EMAIL)
            .update();
    }

    @Test
    void createsAndRevokesInvitation() {
        UUID adminId = createAdmin();
        UUID invitationId = UUID.randomUUID();

        String tokenHash = "a".repeat(64);

        repository.create(
            invitationId,
            adminId,
            "teacher@example.com",
            tokenHash,
            Instant.now().plus(Duration.ofDays(7))
        );

        var invitation = repository.findByTokenHash(
            tokenHash
        ).orElseThrow();

        assertThat(invitation.id())
            .isEqualTo(invitationId);

        assertThat(invitation.createdByAdminId())
            .isEqualTo(adminId);

        assertThat(invitation.email())
            .isEqualTo("teacher@example.com");

        assertThat(invitation.isActive(Instant.now()))
            .isTrue();

        assertThat(repository.findAllByAdminId(adminId))
            .hasSize(1);

        assertThat(
            repository.revokeActive(invitationId, adminId)
        ).isEqualTo(1);

        assertThat(
            repository.revokeActive(invitationId, adminId)
        ).isZero();

        var revoked = repository.findByTokenHash(
            tokenHash
        ).orElseThrow();

        assertThat(revoked.revokedAt())
            .isNotNull();

        assertThat(revoked.isActive(Instant.now()))
            .isFalse();
    }

    @Test
    void acceptedInvitationCannotBeRevokedOrAcceptedAgain() {
        UUID adminId = createAdmin();
        UUID invitationId = UUID.randomUUID();

        String tokenHash = "b".repeat(64);

        repository.create(
            invitationId,
            adminId,
            "another-teacher@example.com",
            tokenHash,
            Instant.now().plus(Duration.ofDays(7))
        );

        assertThat(
            repository.markAccepted(invitationId)
        ).isEqualTo(1);

        assertThat(
            repository.markAccepted(invitationId)
        ).isZero();

        assertThat(
            repository.revokeActive(invitationId, adminId)
        ).isZero();

        var invitation = repository.findByTokenHash(
            tokenHash
        ).orElseThrow();

        assertThat(invitation.acceptedAt())
            .isNotNull();

        assertThat(invitation.isActive(Instant.now()))
            .isFalse();
    }

    private UUID createAdmin() {
        UUID adminId = UUID.randomUUID();

        jdbcClient.sql("""
                INSERT INTO users (
                    id,
                    email,
                    status
                )
                VALUES (
                    :id,
                    :email,
                    'ACTIVE'
                )
                """)
            .param("id", adminId)
            .param("email", ADMIN_EMAIL)
            .update();

        return adminId;
    }
}
