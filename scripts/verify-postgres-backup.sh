#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

usage() {
    cat <<'USAGE'
Usage: ./scripts/verify-postgres-backup.sh /path/to/backup

Verify a PostgreSQL 16 pg_dump in a disposable local Docker container.
Supports custom archives and plain SQL dumps, optionally gzip-compressed.
No production connection, published ports, or persistent volumes are used.
Optional: RESTORE_DRILL_EXPECTED_MIGRATIONS=N requires exactly N applied SQL migrations.
Optional: RESTORE_DRILL_OWNER_ROLE=role creates a local NOLOGIN role for SQL ownership statements.
Optional: RESTORE_DRILL_TMPFS_SIZE=2g controls disposable PGDATA capacity (positive m/g size).
USAGE
}

fail() {
    printf 'ERROR: %s\n' "$2" >&2
    exit "$1"
}

if [[ ${1:-} == --help || ${1:-} == -h ]]; then
    usage
    exit 0
fi
[[ $# == 1 ]] || { usage >&2; exit 2; }
expected=${RESTORE_DRILL_EXPECTED_MIGRATIONS:-}
[[ -z $expected || $expected =~ ^[1-9][0-9]*$ ]] || fail 2 'Expected migrations must be a positive integer.'
owner_role=${RESTORE_DRILL_OWNER_ROLE:-}
[[ -z $owner_role || $owner_role =~ ^[a-zA-Z_][a-zA-Z0-9_]{0,62}$ ]] || fail 2 'Owner role must be a simple PostgreSQL identifier (up to 63 characters).'
[[ $owner_role != restore_drill_admin ]] || fail 2 'Owner role must differ from the disposable admin role.'
tmpfs_size=${RESTORE_DRILL_TMPFS_SIZE:-2g}
[[ $tmpfs_size =~ ^[1-9][0-9]*[mg]$ ]] || fail 2 'tmpfs size must be a positive m/g value, for example 2048m or 2g.'
backup=$1
[[ -f $backup && -r $backup && -s $backup ]] || fail 2 'Backup must be a readable, non-empty regular file.'
for tool in docker mktemp od head gzip grep tr env; do
    command -v "$tool" >/dev/null || fail 3 "Missing prerequisite: $tool"
done

# Resolve the effective Docker endpoint, then pin every command to that local socket.
# In particular, refuse an SSH/TCP context even if it is the user's current context.
if [[ -n ${DOCKER_HOST:-} && -z ${DOCKER_CONTEXT:-} ]]; then
    endpoint=$DOCKER_HOST
else
    endpoint=$(docker context inspect "$(docker context show)" --format '{{.Endpoints.docker.Host}}') \
        || fail 3 'Cannot resolve Docker context.'
fi
[[ $endpoint == unix:///* ]] || fail 3 'Only a local Docker Unix socket is allowed.'
docker_local() {
    env -u DOCKER_HOST -u DOCKER_CONTEXT docker --host "$endpoint" "$@"
}
docker_local info >/dev/null 2>&1 || fail 3 'Local Docker daemon is unavailable.'

container_id=''
workdir=''
cleanup() {
    local status=$?
    trap - EXIT HUP INT TERM
    if [[ -n $container_id ]]; then
        if ! docker_local rm --force --volumes "$container_id" >/dev/null; then
            printf 'ERROR: Cleanup failed; remove disposable container %s manually.\n' "$container_id" >&2
            [[ $status != 0 ]] || status=6
        fi
    fi
    if [[ -n $workdir ]]; then
        if ! rm -rf -- "$workdir"; then
            printf 'ERROR: Cannot remove private temporary directory %s.\n' "$workdir" >&2
            [[ $status != 0 ]] || status=6
        fi
    fi
    exit "$status"
}
trap cleanup EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM

workdir=$(mktemp -d "${TMPDIR:-/tmp}/tutor-restore-drill.XXXXXXXX") || fail 3 'Cannot create temporary directory.'
dump=$backup
magic=$(od -An -tx1 -N2 -- "$backup")
if [[ ${magic//[[:space:]]/} == 1f8b ]]; then
    gzip -dc -- "$backup" > "$workdir/dump" || fail 4 'Invalid or truncated gzip backup.'
    dump=$workdir/dump
fi
if [[ $(head -c 5 -- "$dump") == PGDMP ]]; then
    format=custom
elif head -c 1024 -- "$dump" | grep -q '^-- PostgreSQL database dump'; then
    format=sql
else
    fail 4 'Unsupported backup: expected a single-database pg_dump (custom or SQL).'
fi

suffix=$(od -An -N12 -tx1 /dev/urandom | tr -d ' \n')
name="tutor-restore-drill-$suffix"
database="restore_drill_$suffix"
password=$(od -An -N24 -tx1 /dev/urandom | tr -d ' \n')
# tmpfs shadows the image's PGDATA volume. Nothing is mounted from the host.
container_id=$(docker_local create --name "$name" \
    --label tutorplatform.restore-drill=true \
    --network none --log-driver none \
    --tmpfs "/var/lib/postgresql/data:rw,nosuid,noexec,size=$tmpfs_size" \
    --env POSTGRES_USER=restore_drill_admin \
    --env POSTGRES_DB=restore_drill_bootstrap \
    --env "POSTGRES_PASSWORD=$password" \
    --env 'POSTGRES_INITDB_ARGS=--auth-host=reject --auth-local=trust' \
    postgres:16-alpine postgres -c listen_addresses= \
    -c unix_socket_permissions=0700) || fail 3 'Cannot create disposable PostgreSQL container.'
docker_local start "$container_id" >/dev/null || fail 3 'Cannot start disposable PostgreSQL container.'

ready=false
for ((attempt = 0; attempt < 60; attempt++)); do
    # Expand PGDATA in the container, not in the host shell.
    # shellcheck disable=SC2016
    if docker_local exec --user postgres "$container_id" \
        sh -c 'test "$(head -n 1 "$PGDATA/postmaster.pid" 2>/dev/null)" = 1 &&
            pg_isready -h /var/run/postgresql -U restore_drill_admin -d restore_drill_bootstrap' >/dev/null 2>&1; then
        ready=true
        break
    fi
    sleep 1
done
[[ $ready == true ]] || fail 3 'Disposable PostgreSQL did not become ready in 60 seconds.'
docker_local exec --user postgres "$container_id" \
    createdb -h /var/run/postgresql -U restore_drill_admin --template template0 "$database" \
    || fail 3 'Cannot create isolated restore database.'

if [[ -n $owner_role ]]; then
    docker_local exec --interactive --user postgres "$container_id" \
        psql -X -h /var/run/postgresql -U restore_drill_admin -d "$database" \
        --set ON_ERROR_STOP=1 --set "owner_role=$owner_role" --quiet <<'SQL' \
        || fail 3 'Cannot create local dump owner role.'
CREATE ROLE :"owner_role" NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
SQL
fi

printf 'Restoring %s dump into %s (%s).\n' "$format" "$database" "$name"
if [[ $format == custom ]]; then
    docker_local exec --interactive --user postgres "$container_id" \
        pg_restore -h /var/run/postgresql -U restore_drill_admin -d "$database" \
        --no-owner --no-privileges --exit-on-error --single-transaction < "$dump" \
        || fail 4 'pg_restore failed.'
else
    docker_local exec --interactive --user postgres "$container_id" \
        psql -X -h /var/run/postgresql -U restore_drill_admin -d "$database" \
        --set ON_ERROR_STOP=1 --single-transaction --quiet < "$dump" \
        || fail 4 'SQL restore failed (check owner role requirements; --create dumps are not supported).'
fi
# No application startup or migration execution: inspect the restored snapshot as-is.
psql_restore() {
    docker_local exec --interactive --user postgres "$container_id" \
        psql -X -h /var/run/postgresql -U restore_drill_admin -d "$database" \
        --set ON_ERROR_STOP=1 --no-align --tuples-only "$@"
}
psql_restore --quiet > /dev/null <<'SQL' || fail 5 'Restored schema or core table query failed.'
BEGIN READ ONLY;
SET LOCAL statement_timeout = '30s';
SELECT 1;
DO $$
DECLARE
    table_name text;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'public' AND c.relname = 'flyway_schema_history'
          AND c.relkind IN ('r', 'p')
    ) THEN
        RAISE EXCEPTION 'Missing public.flyway_schema_history';
    END IF;
    FOREACH table_name IN ARRAY ARRAY[
        'users', 'teachers', 'students', 'subjects', 'learning_programs',
        'topics', 'tasks', 'lesson_sessions', 'homeworks', 'submissions'
    ] LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE n.nspname = 'public' AND c.relname = table_name AND c.relkind IN ('r', 'p')
        ) THEN
            RAISE EXCEPTION 'Missing core table public.%', table_name;
        END IF;
        -- Bounded reads succeed for an empty database; never print application rows.
        EXECUTE format('SELECT id FROM public.%I LIMIT 1', table_name);
    END LOOP;
    IF EXISTS (SELECT 1 FROM public.flyway_schema_history WHERE NOT success) THEN
        RAISE EXCEPTION 'Flyway history contains failed migrations';
    END IF;
END $$;
COMMIT;
SQL
applied=$(psql_restore --command "SELECT count(*) FROM public.flyway_schema_history WHERE success AND version IS NOT NULL AND type = 'SQL';") \
    || fail 5 'Cannot read Flyway migration count.'
[[ $applied =~ ^[0-9]+$ && $applied != 0 ]] || fail 5 'No successfully applied versioned SQL migrations.'
if [[ -n $expected && $applied != "$expected" ]]; then
    fail 5 "Applied migrations: $applied; expected: $expected."
fi
printf 'Verified: database opens; Flyway history and 10 core tables are readable.\n'
printf 'Applied SQL migrations: %s\n' "$applied"
printf 'Restore verification passed (cleanup runs before exit).\n'
