-- Human-readable URLs for learning programs and topics.

ALTER TABLE learning_programs
    ADD COLUMN slug varchar(240);

ALTER TABLE topics
    ADD COLUMN slug varchar(240),
    ADD COLUMN learning_program_id uuid;


-- Transliteration and slug generation.

CREATE FUNCTION tutor_slugify(value text)
RETURNS text
LANGUAGE plpgsql
IMMUTABLE
AS $body$
DECLARE
    letters text[] := ARRAY[
        'а','б','в','г','д','е','ё','ж','з','и','й',
        'к','л','м','н','о','п','р','с','т','у','ф',
        'х','ц','ч','ш','щ','ъ','ы','ь','э','ю','я'
    ];

    replacements text[] := ARRAY[
        'a','b','v','g','d','e','yo','zh','z','i','y',
        'k','l','m','n','o','p','r','s','t','u','f',
        'h','ts','ch','sh','shch','','y','','e','yu','ya'
    ];

    result text := lower(coalesce(value, ''));
    n integer;
BEGIN
    FOR n IN 1 .. array_length(letters, 1) LOOP
        result := replace(result, letters[n], replacements[n]);
    END LOOP;

    result := trim(
        both '-'
        from regexp_replace(result, '[^a-z0-9]+', '-', 'g')
    );

    result := trim(both '-' from left(result, 190));

    RETURN coalesce(nullif(result, ''), 'item');
END;
$body$;


-- Backfill existing learning programs.

DO $body$
DECLARE
    row_data record;
    stem text;
    candidate text;
    suffix integer;
BEGIN
    FOR row_data IN
        SELECT id, teacher_id, title
        FROM learning_programs
        ORDER BY created_at, id
    LOOP
        stem := tutor_slugify(row_data.title);
        candidate := stem;
        suffix := 2;

        WHILE EXISTS (
            SELECT 1
            FROM learning_programs
            WHERE teacher_id = row_data.teacher_id
              AND slug = candidate
        ) LOOP
            candidate := stem || '-' || suffix;
            suffix := suffix + 1;
        END LOOP;

        UPDATE learning_programs
        SET slug = candidate
        WHERE id = row_data.id;
    END LOOP;
END;
$body$;


-- Backfill existing topics.

DO $body$
DECLARE
    row_data record;
    stem text;
    candidate text;
    suffix integer;
BEGIN
    FOR row_data IN
        SELECT
            topic.id,
            module.learning_program_id AS program_id,
            topic.title

        FROM topics topic

        JOIN modules module
            ON module.id = topic.module_id

        ORDER BY topic.created_at, topic.id
    LOOP
        stem := tutor_slugify(row_data.title);
        candidate := stem;
        suffix := 2;

        WHILE EXISTS (
            SELECT 1
            FROM topics
            WHERE learning_program_id = row_data.program_id
              AND slug = candidate
        ) LOOP
            candidate := stem || '-' || suffix;
            suffix := suffix + 1;
        END LOOP;

        UPDATE topics
        SET
            slug = candidate,
            learning_program_id = row_data.program_id
        WHERE id = row_data.id;
    END LOOP;
END;
$body$;


-- Constraints.

ALTER TABLE learning_programs
    ALTER COLUMN slug SET NOT NULL;

ALTER TABLE topics
    ALTER COLUMN slug SET NOT NULL,
    ALTER COLUMN learning_program_id SET NOT NULL;


ALTER TABLE modules
    ADD CONSTRAINT uq_modules_id_program
    UNIQUE (id, learning_program_id);


ALTER TABLE topics
    ADD CONSTRAINT fk_topics_module_program
    FOREIGN KEY (module_id, learning_program_id)
    REFERENCES modules (id, learning_program_id)
    ON DELETE RESTRICT;


CREATE UNIQUE INDEX uq_learning_program_teacher_slug
    ON learning_programs (teacher_id, slug);


CREATE UNIQUE INDEX uq_topic_program_slug
    ON topics (learning_program_id, slug);


-- Automatically generate slugs for new programs.

CREATE FUNCTION tutor_program_slug_trigger()
RETURNS trigger
LANGUAGE plpgsql
AS $body$
DECLARE
    stem text;
    candidate text;
    suffix integer := 2;
BEGIN
    IF TG_OP = 'UPDATE' THEN
        NEW.slug := OLD.slug;
        RETURN NEW;
    END IF;

    PERFORM pg_advisory_xact_lock(
        hashtextextended(
            'program-slug:' || NEW.teacher_id::text,
            0
        )
    );

    stem := tutor_slugify(NEW.title);
    candidate := stem;

    WHILE EXISTS (
        SELECT 1
        FROM learning_programs
        WHERE teacher_id = NEW.teacher_id
          AND slug = candidate
    ) LOOP
        candidate := stem || '-' || suffix;
        suffix := suffix + 1;
    END LOOP;

    NEW.slug := candidate;

    RETURN NEW;
END;
$body$;


CREATE TRIGGER trg_program_slug

BEFORE INSERT OR UPDATE OF title, slug
ON learning_programs

FOR EACH ROW
EXECUTE FUNCTION tutor_program_slug_trigger();


-- Automatically generate slugs for new topics.

CREATE FUNCTION tutor_topic_slug_trigger()
RETURNS trigger
LANGUAGE plpgsql
AS $body$
DECLARE
    stem text;
    candidate text;
    suffix integer := 2;
BEGIN
    IF TG_OP = 'UPDATE'
       AND NEW.module_id <> OLD.module_id
    THEN
        RAISE EXCEPTION
            'Moving topics between modules is not supported';
    END IF;

    SELECT learning_program_id
    INTO NEW.learning_program_id
    FROM modules
    WHERE id = NEW.module_id;

    IF NEW.learning_program_id IS NULL THEN
        RAISE EXCEPTION 'Unknown topic module';
    END IF;

    IF TG_OP = 'UPDATE' THEN
        NEW.slug := OLD.slug;
        RETURN NEW;
    END IF;

    PERFORM pg_advisory_xact_lock(
        hashtextextended(
            'topic-slug:' || NEW.learning_program_id::text,
            0
        )
    );

    stem := tutor_slugify(NEW.title);
    candidate := stem;

    WHILE EXISTS (
        SELECT 1
        FROM topics
        WHERE learning_program_id = NEW.learning_program_id
          AND slug = candidate
    ) LOOP
        candidate := stem || '-' || suffix;
        suffix := suffix + 1;
    END LOOP;

    NEW.slug := candidate;

    RETURN NEW;
END;
$body$;


CREATE TRIGGER trg_topic_slug

BEFORE INSERT OR UPDATE OF title, slug, module_id
ON topics

FOR EACH ROW
EXECUTE FUNCTION tutor_topic_slug_trigger();
