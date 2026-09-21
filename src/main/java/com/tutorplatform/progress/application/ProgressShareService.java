package com.tutorplatform.progress.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.application.ProgramQuery;
import com.tutorplatform.progress.api.request.CreateProgressShareRequest;
import com.tutorplatform.progress.api.response.ProgressShareCreatedResponse;
import com.tutorplatform.progress.api.response.ProgressShareListResponse;
import com.tutorplatform.progress.api.response.ProgressShareSummaryResponse;
import com.tutorplatform.progress.application.exception.InvalidProgressShareExpirationException;
import com.tutorplatform.progress.application.exception.ProgressShareNotFoundException;
import com.tutorplatform.progress.application.exception.ProgressStudentProgramNotFoundException;
import com.tutorplatform.progress.domain.ProgressShare;
import com.tutorplatform.progress.domain.ProgressShareRepository;
import com.tutorplatform.progress.domain.ProgressShareStatus;
import com.tutorplatform.student.application.invite.StudentInviteTokenService;
import com.tutorplatform.student.application.management.StudentNotFoundException;
import com.tutorplatform.student.application.ownership.StudentOwnershipQuery;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgressShareService {

    private final StudentOwnershipQuery studentOwnershipQuery;
    private final ProgramQuery programQuery;
    private final ProgressShareRepository progressShareRepository;
    private final StudentInviteTokenService tokenService;
    private final String publicFrontendBaseUrl;

    public ProgressShareService(
            StudentOwnershipQuery studentOwnershipQuery,
            ProgramQuery programQuery,
            ProgressShareRepository progressShareRepository,
            StudentInviteTokenService tokenService,
            @Value("${app.student-invites.public-frontend-base-url}") URI publicFrontendBaseUrl) {
        this.studentOwnershipQuery = studentOwnershipQuery;
        this.programQuery = programQuery;
        this.progressShareRepository = progressShareRepository;
        this.tokenService = tokenService;
        this.publicFrontendBaseUrl = stripTrailingSlash(publicFrontendBaseUrl.toString());
    }

    @Transactional
    public ProgressShareCreatedResponse create(
            AuthenticatedUser principal, UUID studentId, CreateProgressShareRequest request) {
        UUID teacherId = requireOwnedStudent(principal, studentId);
        requireAccessibleProgram(teacherId, studentId, request.studentProgramId());

        Instant now = Instant.now();
        if (request.expiresAt() != null && !request.expiresAt().isAfter(now)) {
            throw new InvalidProgressShareExpirationException();
        }

        StudentInviteTokenService.Token token = tokenService.createToken();
        ProgressShare share =
                progressShareRepository.saveAndFlush(
                        new ProgressShare(
                                UUID.randomUUID(),
                                request.studentProgramId(),
                                teacherId,
                                token.hash(),
                                request.expiresAt(),
                                null,
                                null));

        return new ProgressShareCreatedResponse(
                share.id(),
                share.studentProgramId(),
                share.expiresAt(),
                publicFrontendBaseUrl + "/progress/" + token.rawValue(),
                share.createdAt());
    }

    @Transactional(readOnly = true)
    public ProgressShareListResponse list(
            AuthenticatedUser principal, UUID studentId, UUID studentProgramId) {
        UUID teacherId = requireOwnedStudent(principal, studentId);
        if (studentProgramId != null) {
            requireAccessibleProgram(teacherId, studentId, studentProgramId);
        }
        Instant now = Instant.now();
        return new ProgressShareListResponse(
                progressShareRepository
                        .findAllOwnedBy(teacherId, studentId, studentProgramId)
                        .stream()
                        .map(
                                share ->
                                        new ProgressShareSummaryResponse(
                                                share.id(),
                                                share.studentProgramId(),
                                                status(share, now),
                                                share.expiresAt(),
                                                share.revokedAt(),
                                                share.createdAt()))
                        .toList());
    }

    @Transactional
    public void revoke(AuthenticatedUser principal, UUID studentId, UUID shareId) {
        UUID teacherId = requireOwnedStudent(principal, studentId);
        ProgressShare share =
                progressShareRepository
                        .findOwnedById(shareId, teacherId, studentId)
                        .orElseThrow(ProgressShareNotFoundException::new);
        if (share.revokedAt() == null) {
            progressShareRepository.saveAndFlush(share.revoke(Instant.now()));
        }
    }

    private UUID requireOwnedStudent(AuthenticatedUser principal, UUID studentId) {
        UUID teacherId = studentOwnershipQuery.findTeacherIdByUserId(principal.id()).orElseThrow();
        if (!studentOwnershipQuery.isActivePrimaryOwner(teacherId, studentId)) {
            throw new StudentNotFoundException();
        }
        return teacherId;
    }

    private void requireAccessibleProgram(UUID teacherId, UUID studentId, UUID studentProgramId) {
        ProgramQuery.StudentProgramContext program =
                programQuery
                        .findStudentProgram(studentProgramId)
                        .orElseThrow(ProgressStudentProgramNotFoundException::new);
        if (!program.belongsToStudent(studentId) || !program.isAssignedBy(teacherId)) {
            throw new ProgressStudentProgramNotFoundException();
        }
    }

    private ProgressShareStatus status(ProgressShare share, Instant now) {
        if (share.revokedAt() != null) {
            return ProgressShareStatus.REVOKED;
        }
        if (share.expiresAt() != null && share.expiresAt().isBefore(now)) {
            return ProgressShareStatus.EXPIRED;
        }
        return ProgressShareStatus.ACTIVE;
    }

    private static String stripTrailingSlash(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }
}
