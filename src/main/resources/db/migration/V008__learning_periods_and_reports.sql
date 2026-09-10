CREATE TABLE progress_shares
(
    id                    uuid PRIMARY KEY,
    student_program_id    uuid         NOT NULL REFERENCES student_programs (id) ON DELETE RESTRICT,
    created_by_teacher_id uuid         NOT NULL REFERENCES teachers (id) ON DELETE RESTRICT,
    token_hash            varchar(128) NOT NULL UNIQUE,
    expires_at            timestamptz,
    revoked_at            timestamptz,
    created_at            timestamptz  NOT NULL DEFAULT now()
);
