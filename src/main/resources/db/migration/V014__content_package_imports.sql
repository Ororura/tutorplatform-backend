ALTER TABLE learning_programs
    ADD CONSTRAINT uq_learning_program_id_teacher UNIQUE (id, teacher_id);

CREATE TABLE content_package_imports
(
    id                  uuid PRIMARY KEY,
    teacher_id          uuid        NOT NULL REFERENCES teachers (id) ON DELETE RESTRICT,
    learning_program_id uuid        NOT NULL REFERENCES learning_programs (id) ON DELETE RESTRICT,
    confirmation_id     uuid        NOT NULL,
    package_digest      varchar(64) NOT NULL,
    module_count        integer     NOT NULL,
    topic_count         integer     NOT NULL,
    material_count      integer     NOT NULL,
    created_module_ids  uuid[]      NOT NULL,
    created_at          timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_content_package_import_confirmation
        UNIQUE (teacher_id, learning_program_id, confirmation_id),
    CONSTRAINT fk_content_package_import_program_owner
        FOREIGN KEY (learning_program_id, teacher_id)
        REFERENCES learning_programs (id, teacher_id) ON DELETE RESTRICT,
    CONSTRAINT ck_content_package_import_digest
        CHECK (package_digest ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_content_package_import_counts
        CHECK (module_count > 0 AND topic_count >= 0 AND material_count >= 0
               AND cardinality(created_module_ids) = module_count)
);
