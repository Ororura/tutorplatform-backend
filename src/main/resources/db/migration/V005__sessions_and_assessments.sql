CREATE TABLE lesson_sessions (
    id uuid PRIMARY KEY,
    student_program_id uuid NOT NULL REFERENCES student_programs(id),
    teacher_id uuid NOT NULL REFERENCES teachers(id),
    started_at timestamptz NOT NULL,
    duration_minutes integer NOT NULL,
    attendance_status varchar(24) NOT NULL,
    summary text,
    private_notes text,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_session_duration CHECK (duration_minutes BETWEEN 1 AND 600),
    CONSTRAINT ck_session_attendance CHECK (
        attendance_status IN ('ATTENDED', 'MISSED', 'CANCELLED')
    )
);

CREATE TABLE lesson_session_topics (
    lesson_session_id uuid NOT NULL REFERENCES lesson_sessions(id) ON DELETE CASCADE,
    topic_id uuid NOT NULL REFERENCES topics(id),
    is_primary boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (lesson_session_id, topic_id)
);

CREATE TABLE teacher_assessments (
    id uuid PRIMARY KEY,
    lesson_session_id uuid NOT NULL UNIQUE REFERENCES lesson_sessions(id),
    understanding_score smallint,
    independence_score smallint,
    practice_score smallint,
    homework_score smallint,
    public_comment text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_teacher_assessment_understanding CHECK (understanding_score BETWEEN 1 AND 5),
    CONSTRAINT ck_teacher_assessment_independence CHECK (independence_score BETWEEN 1 AND 5),
    CONSTRAINT ck_teacher_assessment_practice CHECK (practice_score BETWEEN 1 AND 5),
    CONSTRAINT ck_teacher_assessment_homework CHECK (homework_score BETWEEN 1 AND 5)
);
