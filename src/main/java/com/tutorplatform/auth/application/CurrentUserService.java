package com.tutorplatform.auth.application;

import com.tutorplatform.auth.api.CurrentUserResponse;
import com.tutorplatform.auth.api.UserRole;
import com.tutorplatform.auth.infrastructure.persistence.CurrentUserQueryRepository;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.user.domain.UserEntity;
import com.tutorplatform.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;
    private final CurrentUserQueryRepository currentUserQueryRepository;

    public CurrentUserService(
            UserRepository userRepository,
            CurrentUserQueryRepository currentUserQueryRepository
    ) {
        this.userRepository = userRepository;
        this.currentUserQueryRepository = currentUserQueryRepository;
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(AuthenticatedUser principal) {
        UserEntity user = userRepository.findById(principal.id()).orElseThrow();
        String displayName = currentUserQueryRepository.findDisplayName(user.getId()).orElseThrow();

        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                displayName,
                user.getRoles().stream()
                        .map(role -> UserRole.valueOf(role.name()))
                        .sorted(Comparator.comparing(UserRole::name))
                        .toList()
        );
    }
}
