package com.tutorplatform.progress.application;

import com.tutorplatform.progress.api.response.PublicCurrentProgressResponse;
import com.tutorplatform.progress.application.exception.ProgressShareExpiredException;
import com.tutorplatform.progress.application.exception.ProgressShareNotFoundException;
import com.tutorplatform.progress.application.exception.ProgressShareRevokedException;
import com.tutorplatform.progress.domain.ProgressShare;
import com.tutorplatform.progress.domain.ProgressShareRepository;
import com.tutorplatform.student.application.StudentInviteTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class PublicProgressShareService {

    private final StudentInviteTokenService tokenService;
    private final ProgressShareRepository progressShareRepository;
    private final GetCurrentProgressService getCurrentProgressService;

    public PublicProgressShareService(
        StudentInviteTokenService tokenService,
        ProgressShareRepository progressShareRepository,
        GetCurrentProgressService getCurrentProgressService
    ) {
        this.tokenService = tokenService;
        this.progressShareRepository = progressShareRepository;
        this.getCurrentProgressService = getCurrentProgressService;
    }

    @Transactional(readOnly = true)
    public PublicCurrentProgressResponse get(String rawToken) {
        ProgressShare share = progressShareRepository.findByTokenHash(tokenService.hash(rawToken))
            .orElseThrow(ProgressShareNotFoundException::new);
        validateState(share, Instant.now());
        return PublicCurrentProgressResponse.from(
            getCurrentProgressService.getCurrentProgress(share.studentProgramId())
        );
    }

    private void validateState(ProgressShare share, Instant now) {
        if (share.revokedAt() != null) {
            throw new ProgressShareRevokedException();
        }
        if (share.expiresAt() != null && share.expiresAt().isBefore(now)) {
            throw new ProgressShareExpiredException();
        }
    }
}
