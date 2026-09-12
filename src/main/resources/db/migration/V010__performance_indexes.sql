CREATE INDEX ix_student_program_student_status
    ON student_programs (student_id, status);

CREATE INDEX ix_topic_progress_program_status
    ON student_topic_progress (student_program_id, status);

CREATE INDEX ix_session_program_started
    ON lesson_sessions (student_program_id, started_at DESC);

CREATE INDEX ix_session_teacher_started
    ON lesson_sessions (teacher_id, started_at DESC);

CREATE INDEX ix_task_teacher_subject_status
    ON tasks (teacher_id, subject_id, status);

CREATE INDEX ix_homework_program_assigned
    ON homeworks (student_program_id, assigned_at DESC);

CREATE INDEX ix_homework_program_due_active
    ON homeworks (student_program_id, due_at)
    WHERE status = 'ASSIGNED';

CREATE INDEX ix_submission_program_submitted
    ON submissions (student_program_id, submitted_at DESC);

CREATE INDEX ix_submission_student_task_submitted
    ON submissions (student_id, task_id, submitted_at DESC);

CREATE INDEX ix_submission_homework_item_submitted
    ON submissions (homework_item_id, submitted_at DESC)
    WHERE homework_item_id IS NOT NULL;

CREATE INDEX ix_report_program_created
    ON progress_reports (student_program_id, created_at DESC);
