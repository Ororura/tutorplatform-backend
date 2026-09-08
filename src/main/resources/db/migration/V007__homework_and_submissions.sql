CREATE TABLE homeworks (
    id uuid PRIMARY KEY,
    student_program_id uuid NOT NULL REFERENCES student_programs(id),
    assigned_by_teacher_id uuid NOT NULL REFERENCES teachers(id),
    title varchar(220) NOT NULL,
    description text,
    assigned_at timestamptz NOT NULL DEFAULT now(),
    due_at timestamptz,
    status varchar(24) NOT NULL DEFAULT 'ASSIGNED',
    completed_at timestamptz,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_homeworks_status CHECK (status IN ('ASSIGNED', 'COMPLETED', 'CANCELLED'))
);

CREATE TABLE homework_items (
    id uuid PRIMARY KEY,
    homework_id uuid NOT NULL REFERENCES homeworks(id) ON DELETE CASCADE,
    task_id uuid NOT NULL REFERENCES tasks(id),
    position integer NOT NULL,
    required boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_homework_items_position CHECK (position >= 0),
    CONSTRAINT uq_homework_items_homework_position UNIQUE (homework_id, position)
);

CREATE TABLE submissions (
    id uuid PRIMARY KEY,
    student_id uuid NOT NULL REFERENCES students(id),
    student_program_id uuid NOT NULL REFERENCES student_programs(id),
    task_id uuid NOT NULL REFERENCES tasks(id),
    homework_item_id uuid REFERENCES homework_items(id),
    attempt_no integer NOT NULL,
    status varchar(24) NOT NULL,
    text_answer text,
    submitted_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_submissions_attempt_no CHECK (attempt_no > 0),
    CONSTRAINT ck_submissions_status CHECK (
        status IN ('SUBMITTED', 'PASSED', 'FAILED', 'NEEDS_REVIEW', 'SYSTEM_ERROR')
    ),
    CONSTRAINT uq_submissions_student_context_attempt UNIQUE NULLS NOT DISTINCT (
        student_id, student_program_id, task_id, homework_item_id, attempt_no
    )
);

CREATE TABLE code_submissions (
    submission_id uuid PRIMARY KEY REFERENCES submissions(id) ON DELETE CASCADE,
    source_code text NOT NULL,
    execution_status varchar(24) NOT NULL,
    passed_tests integer NOT NULL DEFAULT 0,
    total_tests integer NOT NULL DEFAULT 0,
    execution_time_ms integer,
    stdout_excerpt text,
    stderr_excerpt text,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_code_submissions_execution_status CHECK (
        execution_status IN (
            'PENDING', 'RUNNING', 'PASSED', 'FAILED', 'TIMEOUT', 'RUNTIME_ERROR', 'SYSTEM_ERROR'
        )
    ),
    CONSTRAINT ck_code_submissions_passed_tests CHECK (passed_tests >= 0),
    CONSTRAINT ck_code_submissions_total_tests CHECK (total_tests >= 0),
    CONSTRAINT ck_code_submissions_test_counts CHECK (passed_tests <= total_tests),
    CONSTRAINT ck_code_submissions_execution_time CHECK (
        execution_time_ms IS NULL OR execution_time_ms >= 0
    )
);
