#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

usage() {
    cat <<'USAGE'
Usage: ./scripts/verify-postgres-backup.sh /path/to/backup

Verify a PostgreSQL 16 pg_dump in a disposable local Docker container.
Supports custom archives and plain SQL dumps, optionally gzip-compressed.
No production connection, published ports, or persistent volumes are used.
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
backup=$1
[[ -f $backup && -r $backup && -s $backup ]] || fail 2 'Backup must be a readable, non-empty regular file.'
for tool in docker mktemp od head gzip; do
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
    --tmpfs /var/lib/postgresql/data:rw,nosuid,noexec,size=2g \
    --env POSTGRES_USER=restore_drill_admin \
    --env POSTGRES_DB=restore_drill_bootstrap \
    --env "POSTGRES_PASSWORD=$password" \
    --env 'POSTGRES_INITDB_ARGS=--auth-host=reject --auth-local=trust' \
    postgres:16-alpine postgres -c listen_addresses= \
    -c unix_socket_permissions=0700) || fail 3 'Cannot create disposable PostgreSQL container.'
docker_local start "$container_id" >/dev/null || fail 3 'Cannot start disposable PostgreSQL container.'

ready=false
for ((attempt = 0; attempt < 60; attempt++)); do
    if docker_local exec --user postgres "$container_id" \
        pg_isready -h /var/run/postgresql -U restore_drill_admin -d restore_drill_bootstrap >/dev/null 2>&1; then
        ready=true
        break
    fi
    sleep 1
done
[[ $ready == true ]] || fail 3 'Disposable PostgreSQL did not become ready in 60 seconds.'
docker_local exec --user postgres "$container_id" \
    createdb -h /var/run/postgresql -U restore_drill_admin --template template0 "$database" \
    || fail 3 'Cannot create isolated restore database.'

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
        || fail 4 'SQL restore failed (owner roles and --create dumps are not supported).'
fi
printf 'Restore completed.\n'
