CREATE TABLE teacher_registration_invites
(
    id                  uuid PRIMARY KEY,
    created_by_admin_id uuid NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    email               citext NOT NULL,
    token_hash          varchar(64) NOT NULL UNIQUE,
    expires_at          timestamptz NOT NULL,
    accepted_at         timestamptz,
    revoked_at          timestamptz,
    created_at          timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT ck_teacher_registration_invite_token_hash
        CHECK (token_hash ~ '^[0-9a-f]{64}$'),

    CONSTRAINT ck_teacher_registration_invite_expiration
        CHECK (expires_at > created_at),

    CONSTRAINT ck_teacher_registration_invite_state
        CHECK (accepted_at IS NULL OR revoked_at IS NULL)
);

CREATE INDEX ix_teacher_registration_invites_admin_created
    ON teacher_registration_invites (
        created_by_admin_id,
        created_at DESC
    );

CREATE INDEX ix_teacher_registration_invites_email
    ON teacher_registration_invites (email);
