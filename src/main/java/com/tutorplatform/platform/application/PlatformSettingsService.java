package com.tutorplatform.platform.application;

import com.tutorplatform.platform.domain.PlatformSettings;
import com.tutorplatform.platform.domain.RegistrationMode;
import com.tutorplatform.platform.infrastructure.persistence.PlatformSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class PlatformSettingsService {

    private final PlatformSettingsRepository repository;

    public PlatformSettingsService(
        PlatformSettingsRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PlatformSettings getSettings() {
        return repository.getSettings();
    }

    @Transactional
    public PlatformSettings changeRegistrationMode(
        RegistrationMode mode,
        UUID adminId
    ) {
        Objects.requireNonNull(mode);
        Objects.requireNonNull(adminId);

        repository.updateRegistrationMode(
            mode,
            adminId
        );

        return repository.getSettings();
    }
}
