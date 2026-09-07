CREATE TABLE subjects (
    id uuid PRIMARY KEY,
    owner_teacher_id uuid REFERENCES teachers(id) ON DELETE RESTRICT,
    code varchar(64),
    name varchar(160) NOT NULL,
    description text,
    status varchar(24) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_subjects_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE UNIQUE INDEX uq_system_subject_code
    ON subjects(code)
    WHERE owner_teacher_id IS NULL AND code IS NOT NULL;

CREATE UNIQUE INDEX uq_teacher_subject_name
    ON subjects(owner_teacher_id, lower(name))
    WHERE owner_teacher_id IS NOT NULL;

CREATE TABLE learning_programs (
    id uuid PRIMARY KEY,
    teacher_id uuid NOT NULL REFERENCES teachers(id) ON DELETE RESTRICT,
    subject_id uuid NOT NULL REFERENCES subjects(id) ON DELETE RESTRICT,
    title varchar(200) NOT NULL,
    description text,
    status varchar(24) NOT NULL DEFAULT 'DRAFT',
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_learning_program_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'))
);

CREATE TABLE student_programs (
    id uuid PRIMARY KEY,
    student_id uuid NOT NULL REFERENCES students(id) ON DELETE RESTRICT,
    learning_program_id uuid NOT NULL REFERENCES learning_programs(id) ON DELETE RESTRICT,
    assigned_by_teacher_id uuid NOT NULL REFERENCES teachers(id) ON DELETE RESTRICT,
    status varchar(24) NOT NULL DEFAULT 'ACTIVE',
    report_interval_minutes integer NOT NULL DEFAULT 480,
    started_at timestamptz NOT NULL DEFAULT now(),
    completed_at timestamptz,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_student_program_status
        CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'ARCHIVED')),
    CONSTRAINT ck_student_program_report_interval CHECK (report_interval_minutes > 0)
);

CREATE UNIQUE INDEX uq_student_program_active_same_template
    ON student_programs(student_id, learning_program_id)
    WHERE status IN ('ACTIVE', 'PAUSED');

CREATE TABLE modules (
    id uuid PRIMARY KEY,
    learning_program_id uuid NOT NULL REFERENCES learning_programs(id) ON DELETE RESTRICT,
    title varchar(180) NOT NULL,
    description text,
    position integer NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_modules_position CHECK (position >= 0),
    CONSTRAINT uq_modules_program_position UNIQUE (learning_program_id, position)
);

CREATE TABLE topics (
    id uuid PRIMARY KEY,
    module_id uuid NOT NULL REFERENCES modules(id) ON DELETE RESTRICT,
    title varchar(180) NOT NULL,
    description text,
    position integer NOT NULL,
    status varchar(24) NOT NULL DEFAULT 'DRAFT',
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_topics_position CHECK (position >= 0),
    CONSTRAINT ck_topics_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    CONSTRAINT uq_topics_module_position UNIQUE (module_id, position)
);

CREATE TABLE student_topic_progress (
    student_program_id uuid NOT NULL REFERENCES student_programs(id) ON DELETE RESTRICT,
    topic_id uuid NOT NULL REFERENCES topics(id) ON DELETE RESTRICT,
    status varchar(24) NOT NULL DEFAULT 'LOCKED',
    started_at timestamptz,
    completed_at timestamptz,
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (student_program_id, topic_id),
    CONSTRAINT ck_student_topic_progress_status
        CHECK (status IN ('LOCKED', 'AVAILABLE', 'IN_PROGRESS', 'COMPLETED'))
);
