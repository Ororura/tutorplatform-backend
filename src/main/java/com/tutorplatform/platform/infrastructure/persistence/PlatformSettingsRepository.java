package com.tutorplatform.platform.infrastructure.persistence;

import com.tutorplatform.platform.domain.PlatformSettings;
import com.tutorplatform.platform.domain.RegistrationMode;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class PlatformSettingsRepository {

    private final JdbcClient jdbcClient;

    public PlatformSettingsRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public PlatformSettings getSettings() {
        return jdbcClient.sql("""
                SELECT
                    registration_mode,
                    updated_at,
                    updated_by_admin_id
                FROM platform_settings
                WHERE id = 1
                """)
            .query((rs, rowNum) -> new PlatformSettings(
                RegistrationMode.valueOf(
                    rs.getString("registration_mode")
                ),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getObject("updated_by_admin_id", UUID.class)
            ))
            .single();
    }

    public void updateRegistrationMode(
        RegistrationMode mode,
        UUID adminId
    ) {
        int updated = jdbcClient.sql("""
                UPDATE platform_settings
                SET
                    registration_mode = :mode,
                    updated_at = now(),
                    updated_by_admin_id = :adminId
                WHERE id = 1
                """)
            .param("mode", mode.name())
            .param("adminId", adminId)
            .update();

        if (updated != 1) {
            throw new IllegalStateException(
                "Platform settings row is missing"
            );
        }
    }
}
