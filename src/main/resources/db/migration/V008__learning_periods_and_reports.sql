CREATE TABLE learning_periods
(
    id                       uuid PRIMARY KEY,
    student_program_id       uuid        NOT NULL REFERENCES student_programs (id) ON DELETE RESTRICT,
    sequence_no              integer     NOT NULL,
    start_cumulative_minutes integer     NOT NULL,
    target_duration_minutes  integer     NOT NULL,
    end_cumulative_minutes   integer,
    status                   varchar(24) NOT NULL DEFAULT 'ACTIVE',
    started_at               timestamptz,
    completed_at             timestamptz,
    created_at               timestamptz NOT NULL DEFAULT now(),
    updated_at               timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_learning_period_sequence UNIQUE (student_program_id, sequence_no),
    CONSTRAINT ck_learning_period_sequence CHECK (sequence_no > 0),
    CONSTRAINT ck_learning_period_start_minutes CHECK (start_cumulative_minutes >= 0),
    CONSTRAINT ck_learning_period_target_minutes CHECK (target_duration_minutes > 0),
    CONSTRAINT ck_learning_period_status CHECK (status IN ('ACTIVE', 'COMPLETED')),
    CONSTRAINT ck_learning_period_completion CHECK (
        (status = 'ACTIVE' AND end_cumulative_minutes IS NULL AND completed_at IS NULL)
        OR
        (status = 'COMPLETED' AND end_cumulative_minutes IS NOT NULL
            AND end_cumulative_minutes >= start_cumulative_minutes AND completed_at IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_learning_period_active
    ON learning_periods (student_program_id)
    WHERE status = 'ACTIVE';

CREATE TABLE progress_reports
(
    id                      uuid PRIMARY KEY,
    student_program_id      uuid         NOT NULL REFERENCES student_programs (id) ON DELETE RESTRICT,
    learning_period_id      uuid UNIQUE REFERENCES learning_periods (id) ON DELETE RESTRICT,
    generated_by_teacher_id uuid         NOT NULL REFERENCES teachers (id) ON DELETE RESTRICT,
    status                  varchar(24)  NOT NULL DEFAULT 'DRAFT',
    period_started_at       timestamptz  NOT NULL,
    period_ended_at         timestamptz  NOT NULL,
    learning_minutes        integer      NOT NULL,
    snapshot_schema_version smallint     NOT NULL DEFAULT 1,
    snapshot_json           jsonb        NOT NULL DEFAULT '{}'::jsonb,
    teacher_summary         text,
    next_period_plan        text,
    published_at            timestamptz,
    version                 bigint       NOT NULL DEFAULT 0,
    created_at              timestamptz  NOT NULL DEFAULT now(),
    updated_at              timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_progress_report_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT ck_progress_report_learning_minutes CHECK (learning_minutes >= 0),
    CONSTRAINT ck_progress_report_snapshot_version CHECK (snapshot_schema_version > 0)
);

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

CREATE TABLE report_shares
(
    id                    uuid PRIMARY KEY,
    report_id             uuid         NOT NULL REFERENCES progress_reports (id) ON DELETE CASCADE,
    created_by_teacher_id uuid         NOT NULL REFERENCES teachers (id) ON DELETE RESTRICT,
    token_hash            varchar(128) NOT NULL UNIQUE,
    expires_at            timestamptz,
    revoked_at            timestamptz,
    created_at            timestamptz  NOT NULL DEFAULT now()
);
