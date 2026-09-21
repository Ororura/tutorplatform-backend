package com.tutorplatform.platform.application;

import com.tutorplatform.platform.domain.RegistrationMode;
import com.tutorplatform.platform.infrastructure.persistence.PlatformSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationPolicyService {

    private final PlatformSettingsRepository repository;

    public RegistrationPolicyService(PlatformSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void requireOpenRegistration() {

        RegistrationMode mode = repository.getRegistrationModeForRegistration();

        if (mode != RegistrationMode.OPEN) {
            throw new RegistrationInviteRequiredException();
        }
    }
}
