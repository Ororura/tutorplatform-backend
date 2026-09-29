ALTER TABLE content_package_imports
    ADD COLUMN task_count integer NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_content_package_import_task_count CHECK (task_count >= 0);
