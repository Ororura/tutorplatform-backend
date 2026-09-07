CREATE TABLE students (
    id uuid PRIMARY KEY,
    user_id uuid UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
    first_name varchar(100) NOT NULL,
    last_name varchar(100),
    status varchar(24) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_students_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED'))
);

CREATE TABLE teacher_student_links (
    teacher_id uuid NOT NULL REFERENCES teachers(id) ON DELETE RESTRICT,
    student_id uuid NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    relation_type varchar(24) NOT NULL DEFAULT 'PRIMARY',
    started_at timestamptz NOT NULL DEFAULT now(),
    ended_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (teacher_id, student_id),
    CONSTRAINT ck_teacher_student_relation_type CHECK (relation_type IN ('PRIMARY', 'ASSISTANT')),
    CONSTRAINT ck_teacher_student_dates CHECK (ended_at IS NULL OR ended_at >= started_at)
);

CREATE UNIQUE INDEX uq_student_primary_teacher_active
    ON teacher_student_links(student_id)
    WHERE relation_type = 'PRIMARY' AND ended_at IS NULL;

CREATE TABLE student_invites (
    id uuid PRIMARY KEY,
    student_id uuid NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    created_by_teacher_id uuid NOT NULL REFERENCES teachers(id) ON DELETE RESTRICT,
    email citext NOT NULL,
    token_hash varchar(128) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    accepted_at timestamptz,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_student_invite_expiry CHECK (expires_at > created_at),
    CONSTRAINT ck_student_invite_accept_after_create CHECK (accepted_at IS NULL OR accepted_at >= created_at),
    CONSTRAINT ck_student_invite_revoke_after_create CHECK (revoked_at IS NULL OR revoked_at >= created_at)
);
