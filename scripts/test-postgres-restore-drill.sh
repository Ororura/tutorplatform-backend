#!/usr/bin/env bash
# Integration fixtures only: never read an existing application database or backup.
set -Eeuo pipefail
umask 077
root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
verify="$root/scripts/verify-postgres-backup.sh"
if [[ -n ${DOCKER_HOST:-} && -z ${DOCKER_CONTEXT:-} ]]; then
    endpoint=$DOCKER_HOST
else
    endpoint=$(docker context inspect "$(docker context show)" --format '{{.Endpoints.docker.Host}}')
fi
[[ $endpoint == unix:///* ]] || { echo 'Tests require a local Docker Unix socket.' >&2; exit 1; }
docker_local() {
    env -u DOCKER_HOST -u DOCKER_CONTEXT docker --host "$endpoint" "$@"
}
docker_local info >/dev/null
workdir=$(mktemp -d "${TMPDIR:-/tmp}/tutor-restore-tests.XXXXXXXX")
container_id=''
cleanup() {
    local status=$?
    trap - EXIT HUP INT TERM
    if [[ -n $container_id ]]; then
        docker_local rm --force --volumes "$container_id" >/dev/null || status=1
    fi
    rm -rf -- "$workdir" || status=1
    exit "$status"
}
trap cleanup EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM
baseline=$(docker_local ps --all --quiet --filter label=tutorplatform.restore-drill=true | sort)
container_id=$(docker_local create --network none --log-driver none \
    --label tutorplatform.restore-drill-test=true \
    --tmpfs /var/lib/postgresql/data:rw,nosuid,noexec,size=2g \
    --env POSTGRES_USER=restore_fixture_admin \
    --env POSTGRES_DB=restore_drill_fixture \
    --env POSTGRES_PASSWORD=disposable-fixture-only \
    postgres:16-alpine postgres -c listen_addresses=)
docker_local start "$container_id" >/dev/null
ready=false
for ((attempt = 0; attempt < 60; attempt++)); do
    # PGDATA belongs to the container environment.
    # shellcheck disable=SC2016
    if docker_local exec --user postgres "$container_id" sh -c \
        'test "$(head -n 1 "$PGDATA/postmaster.pid" 2>/dev/null)" = 1 &&
         pg_isready -U restore_fixture_admin -d restore_drill_fixture' >/dev/null 2>&1; then
        ready=true
        break
    fi
    sleep 1
done
[[ $ready == true ]] || { echo 'Fixture PostgreSQL startup failed.' >&2; exit 1; }
fixture_sql() {
    docker_local exec --interactive --user postgres "$container_id" \
        psql -X -U restore_fixture_admin -d restore_drill_fixture --set ON_ERROR_STOP=1 --quiet "$@"
}
# Execute the real repository migration SQL. Only the history metadata is a fixture;
# this script does not replace the project's Flyway integration tests.
fixture_sql <<'SQL'
CREATE TABLE public.flyway_schema_history (
    installed_rank integer PRIMARY KEY,
    version varchar(50), description varchar(200) NOT NULL,
    type varchar(20) NOT NULL, script varchar(1000) NOT NULL,
    checksum integer, installed_by varchar(100) NOT NULL,
    installed_on timestamp NOT NULL DEFAULT now(), execution_time integer NOT NULL,
    success boolean NOT NULL
);
SQL
count=0
for migration in "$root"/src/main/resources/db/migration/V*.sql; do
    fixture_sql < "$migration"
    count=$((count + 1))
    fixture_sql --command "INSERT INTO public.flyway_schema_history VALUES ($count, '$count', 'test fixture', 'SQL', 'test fixture', NULL, 'fixture', now(), 0, true);"
done
# Application users/students remain empty: success must not require demo data.
dump() {
    docker_local exec --user postgres "$container_id" pg_dump \
        -U restore_fixture_admin -d restore_drill_fixture "$@"
}
dump -Fc > "$workdir/valid archive.backup"
dump --no-owner --no-privileges > "$workdir/valid.sql"
dump > "$workdir/owned.sql"
dump --create --no-owner --no-privileges > "$workdir/create.sql"
gzip -c "$workdir/valid.sql" > "$workdir/valid.sql.gz"
gzip -c "$workdir/valid archive.backup" > "$workdir/valid.backup.gz"

assert_case() {
    local expected=$1 label=$2 status=0
    shift 2
    TMPDIR="$workdir" "$@" > "$workdir/result.log" 2>&1 || status=$?
    if [[ $status != "$expected" ]]; then
        cat "$workdir/result.log" >&2
        printf 'FAIL: %s (expected exit %s, got %s)\n' "$label" "$expected" "$status" >&2
        exit 1
    fi
    local remaining
    remaining=$(docker_local ps --all --quiet --filter label=tutorplatform.restore-drill=true | sort)
    [[ $remaining == "$baseline" ]] || { echo "FAIL: leaked container after $label" >&2; exit 1; }
    if compgen -G "$workdir/tutor-restore-drill.*" > /dev/null; then
        echo "FAIL: leaked temporary files after $label" >&2
        exit 1
    fi
    printf 'PASS: %s (exit %s; no leaked verification container)\n' "$label" "$status"
}
assert_case 0 'custom archive with empty core tables' env RESTORE_DRILL_EXPECTED_MIGRATIONS="$count" "$verify" "$workdir/valid archive.backup"
assert_case 0 'SQL preserves its dump format with a disposable owner role' env RESTORE_DRILL_OWNER_ROLE=restore_fixture_admin "$verify" "$workdir/owned.sql"
assert_case 4 'SQL --create cannot restore to its original database name' "$verify" "$workdir/create.sql"
assert_case 4 'SQL with unavailable owner fails' "$verify" "$workdir/owned.sql"
assert_case 0 'plain SQL' "$verify" "$workdir/valid.sql"
assert_case 0 'gzip SQL' "$verify" "$workdir/valid.sql.gz"
assert_case 0 'gzip custom archive' "$verify" "$workdir/valid.backup.gz"
assert_case 5 'migration count mismatch' env RESTORE_DRILL_EXPECTED_MIGRATIONS="$((count + 1))" "$verify" "$workdir/valid archive.backup"
size=$(wc -c < "$workdir/valid archive.backup")
head -c "$((size - 512))" "$workdir/valid archive.backup" > "$workdir/truncated.backup"
assert_case 4 'truncated custom archive' "$verify" "$workdir/truncated.backup"
cat "$workdir/valid.sql" > "$workdir/broken.sql"
printf '\nTHIS IS NOT SQL;\n' >> "$workdir/broken.sql"
assert_case 4 'SQL error stops restore' "$verify" "$workdir/broken.sql"
size=$(wc -c < "$workdir/valid.sql.gz")
head -c "$((size - 8))" "$workdir/valid.sql.gz" > "$workdir/truncated.sql.gz"
assert_case 4 'truncated gzip' "$verify" "$workdir/truncated.sql.gz"
dump -Fc --exclude-table=public.flyway_schema_history > "$workdir/no-history.backup"
assert_case 5 'missing Flyway history' "$verify" "$workdir/no-history.backup"
fixture_sql --command 'ALTER TABLE public.users RENAME TO missing_users;'
dump -Fc > "$workdir/missing-table.backup"
assert_case 5 'missing core table' "$verify" "$workdir/missing-table.backup"
fixture_sql --command 'ALTER TABLE public.missing_users RENAME TO users; UPDATE public.flyway_schema_history SET success = false WHERE installed_rank = 1;'
dump -Fc > "$workdir/failed-history.backup"
assert_case 5 'failed Flyway migration' "$verify" "$workdir/failed-history.backup"
fixture_sql --command 'DELETE FROM public.flyway_schema_history;'
dump -Fc > "$workdir/empty-history.backup"
assert_case 5 'empty migration history' "$verify" "$workdir/empty-history.backup"
printf 'not a pg_dump\n' > "$workdir/unsupported"
assert_case 4 'unsupported format' "$verify" "$workdir/unsupported"
assert_case 2 'missing file' "$verify" "$workdir/does-not-exist"
assert_case 2 'invalid migration count' env RESTORE_DRILL_EXPECTED_MIGRATIONS=abc "$verify" "$workdir/valid.sql"
assert_case 3 'remote Docker endpoint refused' env DOCKER_CONTEXT= DOCKER_HOST=tcp://127.0.0.1:2375 "$verify" "$workdir/valid.sql"
printf 'All restore drill cases passed; fixture migrations: %s.\n' "$count"
