CREATE TABLE platform_settings
(
    id                  smallint PRIMARY KEY DEFAULT 1,
    registration_mode   varchar(24) NOT NULL DEFAULT 'OPEN',
    updated_at          timestamptz NOT NULL DEFAULT now(),
    updated_by_admin_id uuid REFERENCES users (id),

    CONSTRAINT ck_platform_settings_singleton
        CHECK (id = 1),

    CONSTRAINT ck_platform_registration_mode
        CHECK (registration_mode IN ('OPEN', 'INVITE_ONLY'))
);

INSERT INTO platform_settings (
    id,
    registration_mode
)
VALUES (
    1,
    'OPEN'
);
