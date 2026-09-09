CREATE TABLE tasks
(
    id                   uuid PRIMARY KEY,
    teacher_id           uuid         NOT NULL REFERENCES teachers (id) ON DELETE RESTRICT,
    subject_id           uuid         NOT NULL REFERENCES subjects (id) ON DELETE RESTRICT,
    title                varchar(220) NOT NULL,
    description_markdown text         NOT NULL,
    task_type            varchar(32)  NOT NULL,
    difficulty           varchar(16)  NOT NULL DEFAULT 'EASY',
    status               varchar(24)  NOT NULL DEFAULT 'DRAFT',
    version              bigint       NOT NULL DEFAULT 0,
    created_at           timestamptz  NOT NULL DEFAULT now(),
    updated_at           timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_tasks_type CHECK (
        task_type IN ('CODE', 'TEXT', 'SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'FILE_UPLOAD')
        ),
    CONSTRAINT ck_tasks_difficulty CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    CONSTRAINT ck_tasks_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'))
);

CREATE TABLE topic_tasks
(
    topic_id   uuid        NOT NULL REFERENCES topics (id),
    task_id    uuid        NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    position   integer     NOT NULL,
    required   boolean     NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (topic_id, task_id),
    CONSTRAINT uq_topic_tasks_topic_position UNIQUE (topic_id, position),
    CONSTRAINT ck_topic_tasks_position CHECK (position >= 0)
);

CREATE TABLE skills
(
    id          uuid PRIMARY KEY,
    subject_id  uuid         NOT NULL REFERENCES subjects (id),
    code        varchar(100) NOT NULL,
    name        varchar(160) NOT NULL,
    description text,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uq_skills_subject_code UNIQUE (subject_id, code)
);

CREATE TABLE task_skills
(
    task_id    uuid        NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    skill_id   uuid        NOT NULL REFERENCES skills (id),
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (task_id, skill_id)
);

CREATE TABLE programming_task_configs
(
    task_id           uuid PRIMARY KEY REFERENCES tasks (id) ON DELETE CASCADE,
    language          varchar(32) NOT NULL,
    starter_code      text,
    execution_enabled boolean     NOT NULL DEFAULT true,
    time_limit_ms     integer     NOT NULL DEFAULT 5000,
    memory_limit_mb   integer     NOT NULL DEFAULT 128,
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_programming_task_configs_time_limit
        CHECK (time_limit_ms BETWEEN 100 AND 30000),
    CONSTRAINT ck_programming_task_configs_memory_limit
        CHECK (memory_limit_mb BETWEEN 16 AND 1024)
);

CREATE TABLE task_test_cases
(
    id              uuid PRIMARY KEY,
    task_id         uuid        NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    input_text      text,
    expected_output text        NOT NULL,
    hidden          boolean     NOT NULL DEFAULT true,
    comparison_mode varchar(32) NOT NULL DEFAULT 'NORMALIZED',
    position        integer     NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_task_test_cases_comparison_mode
        CHECK (comparison_mode IN ('EXACT', 'NORMALIZED')),
    CONSTRAINT ck_task_test_cases_position CHECK (position >= 0),
    CONSTRAINT uq_task_test_cases_task_position UNIQUE (task_id, position)
);
