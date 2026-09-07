CREATE TABLE file_assets (
    id uuid PRIMARY KEY,
    uploaded_by_teacher_id uuid NOT NULL REFERENCES teachers(id),
    storage_provider varchar(24) NOT NULL,
    storage_key varchar(512) NOT NULL UNIQUE,
    original_filename varchar(255) NOT NULL,
    mime_type varchar(160) NOT NULL,
    size_bytes bigint NOT NULL,
    sha256 varchar(64),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_file_assets_storage_provider
        CHECK (storage_provider IN ('LOCAL', 'S3')),
    CONSTRAINT ck_file_assets_size_bytes CHECK (size_bytes >= 0)
);

CREATE TABLE lesson_materials (
    id uuid PRIMARY KEY,
    topic_id uuid NOT NULL REFERENCES topics(id),
    created_by_teacher_id uuid NOT NULL REFERENCES teachers(id),
    material_type varchar(24) NOT NULL,
    title varchar(200) NOT NULL,
    content text,
    file_asset_id uuid REFERENCES file_assets(id),
    external_url text,
    position integer NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_lesson_materials_type CHECK (
        material_type IN ('MARKDOWN', 'TEXT', 'IMAGE', 'FILE', 'LINK', 'CODE_EXAMPLE')
    ),
    CONSTRAINT ck_lesson_materials_position CHECK (position >= 0),
    CONSTRAINT uq_lesson_materials_topic_position UNIQUE (topic_id, position),
    CONSTRAINT ck_lesson_materials_required_value CHECK (
        (material_type NOT IN ('MARKDOWN', 'TEXT', 'CODE_EXAMPLE') OR content IS NOT NULL)
        AND (material_type NOT IN ('IMAGE', 'FILE') OR file_asset_id IS NOT NULL)
        AND (material_type <> 'LINK' OR external_url IS NOT NULL)
    )
);
