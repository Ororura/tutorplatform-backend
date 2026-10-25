ALTER TABLE code_submissions DROP CONSTRAINT ck_code_submissions_execution_status;
ALTER TABLE code_submissions ADD CONSTRAINT ck_code_submissions_execution_status CHECK (
    execution_status IN ('PENDING', 'RUNNING', 'PASSED', 'FAILED', 'TIMEOUT', 'RUNTIME_ERROR', 'SYSTEM_ERROR', 'SKIPPED')
);

ALTER TABLE submissions ADD COLUMN review_comment varchar(4000);

INSERT INTO subjects (id, owner_teacher_id, code, name, status)
VALUES ('6513554d-dceb-5902-b6df-557c7a94b5c9', NULL, 'WEB', 'Веб-разработка', 'ACTIVE');
