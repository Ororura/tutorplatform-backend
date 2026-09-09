CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE users
(
    id            uuid PRIMARY KEY,
    email         citext      NOT NULL UNIQUE,
    password_hash varchar(255),
    status        varchar(24) NOT NULL DEFAULT 'ACTIVE',
    last_login_at timestamptz,
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'DISABLED', 'PENDING'))
);

CREATE TABLE user_roles
(
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role       varchar(32) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role),
    CONSTRAINT ck_user_roles_role CHECK (role IN ('TEACHER', 'STUDENT', 'PARENT', 'ADMIN'))
);

CREATE TABLE teachers
(
    id           uuid PRIMARY KEY,
    user_id      uuid         NOT NULL UNIQUE REFERENCES users (id) ON DELETE RESTRICT,
    display_name varchar(160) NOT NULL,
    created_at   timestamptz  NOT NULL DEFAULT now(),
    updated_at   timestamptz  NOT NULL DEFAULT now()
);
