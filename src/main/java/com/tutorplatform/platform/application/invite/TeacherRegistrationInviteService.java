package com.tutorplatform.platform.application.invite;

import com.tutorplatform.platform.domain.TeacherRegistrationInvite;
import com.tutorplatform.platform.domain.TeacherRegistrationInviteStatus;
import com.tutorplatform.platform.infrastructure.persistence.TeacherRegistrationInviteRepository;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherRegistrationInviteService {

    private final TeacherRegistrationInviteRepository repository;
    private final TeacherRegistrationInviteTokenService tokenService;
    private final Duration ttl;
    private final String frontendBaseUrl;

    public TeacherRegistrationInviteService(
            TeacherRegistrationInviteRepository repository,
            TeacherRegistrationInviteTokenService tokenService,
            @Value("${app.teacher-invites.ttl}") Duration ttl,
            @Value("${app.teacher-invites.public-frontend-base-url}") URI frontendBaseUrl) {
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Teacher invitation TTL must be positive");
        }

        this.repository = repository;
        this.tokenService = tokenService;
        this.ttl = ttl;
        this.frontendBaseUrl = frontendBaseUrl.toString().replaceAll("/+$", "");
    }

    @Transactional
    public CreatedInvitation createInvitation(UUID adminId, String email) {
        Objects.requireNonNull(adminId);
        Objects.requireNonNull(email);

        String normalizedEmail = email.strip();

        if (normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("Invitation email must not be blank");
        }

        if (repository.existsRegisteredUserByEmail(normalizedEmail)) {
            throw new TeacherInvitationEmailAlreadyRegisteredException();
        }

        TeacherRegistrationInviteTokenService.Token token = tokenService.createToken();

        UUID invitationId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(ttl);

        repository.create(invitationId, adminId, normalizedEmail, token.hash(), expiresAt);

        TeacherRegistrationInvite invitation =
                repository
                        .findByTokenHash(token.hash())
                        .orElseThrow(TeacherInvitationNotFoundException::new);

        String invitationUrl = frontendBaseUrl + "/invite/teacher/" + token.rawValue();

        return new CreatedInvitation(
                invitation.id(),
                invitation.email(),
                invitation.expiresAt(),
                invitationUrl,
                invitation.createdAt());
    }

    @Transactional(readOnly = true)
    public PublicInvitation getPublicInvitation(String rawToken) {
        if (rawToken == null || !rawToken.matches("^[A-Za-z0-9_-]{43}$")) {
            throw new TeacherInvitationNotFoundException();
        }

        String tokenHash = tokenService.hash(rawToken);

        TeacherRegistrationInvite invitation =
                repository
                        .findByTokenHash(tokenHash)
                        .orElseThrow(TeacherInvitationNotFoundException::new);

        return new PublicInvitation(
                invitation.email(), invitation.status(Instant.now()), invitation.expiresAt());
    }

    @Transactional(readOnly = true)
    public List<InvitationSummary> listInvitations(UUID adminId) {
        Objects.requireNonNull(adminId);

        Instant now = Instant.now();

        return repository.findAllByAdminId(adminId).stream()
                .map(
                        invitation ->
                                new InvitationSummary(
                                        invitation.id(),
                                        invitation.email(),
                                        invitation.status(now),
                                        invitation.expiresAt(),
                                        invitation.createdAt()))
                .toList();
    }

    @Transactional
    public void revokeInvitation(UUID adminId, UUID invitationId) {
        Objects.requireNonNull(adminId);
        Objects.requireNonNull(invitationId);

        TeacherRegistrationInvite invitation =
                repository
                        .findByIdAndAdminId(invitationId, adminId)
                        .orElseThrow(TeacherInvitationNotFoundException::new);

        if (invitation.revokedAt() != null) {
            return;
        }

        if (!invitation.isActive(Instant.now())) {
            throw new TeacherInvitationNotActiveException();
        }

        int revoked = repository.revokeActive(invitationId, adminId);

        if (revoked != 1) {
            throw new TeacherInvitationNotActiveException();
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public InvitationForAcceptance lockActiveInvitation(String rawToken) {
        if (rawToken == null || !rawToken.matches("^[A-Za-z0-9_-]{43}$")) {
            throw new TeacherInvitationNotFoundException();
        }

        String tokenHash = tokenService.hash(rawToken);

        TeacherRegistrationInvite invitation =
                repository
                        .findByTokenHashForUpdate(tokenHash)
                        .orElseThrow(TeacherInvitationNotFoundException::new);

        if (!invitation.isActive(Instant.now())) {
            throw new TeacherInvitationNotActiveException();
        }

        return new InvitationForAcceptance(invitation.id(), invitation.email());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void markInvitationAccepted(UUID invitationId) {
        int updated = repository.markAccepted(invitationId);

        if (updated != 1) {
            throw new TeacherInvitationNotActiveException();
        }
    }

    public record InvitationForAcceptance(UUID id, String email) {}

    public record PublicInvitation(
            String email, TeacherRegistrationInviteStatus status, Instant expiresAt) {}

    public record CreatedInvitation(
            UUID id, String email, Instant expiresAt, String invitationUrl, Instant createdAt) {}

    public record InvitationSummary(
            UUID id,
            String email,
            TeacherRegistrationInviteStatus status,
            Instant expiresAt,
            Instant createdAt) {}
}
