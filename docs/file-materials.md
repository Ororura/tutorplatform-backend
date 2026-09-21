# File lesson materials (MVP)

Architecture Design v1 / ER Model v1: the existing content FileAsset metadata and
LessonMaterial tables remain the source of truth. The separate `file` capability
exposes `file.application.FileStorage`; only `file.infrastructure.LocalFileStorage`
knows filesystem locations. Content uses the port. A future S3 adapter implements
that port without changing the content use cases. No program → content dependency
is introduced.

## Teacher API

- `POST /api/v1/teacher/topics/{topicId}/materials/upload`, multipart/form-data:
  `file` (binary part), `materialType` (`FILE` or `IMAGE`), `title`, `position`.
  Requires a Teacher session and CSRF token. Returns the existing LessonMaterial
  response and Location (201); validation 400, size 413, ownership 404, position 409.
- `GET /api/v1/teacher/topics/{topicId}/materials/{materialId}/download`:
  checks ownership, returns bytes with MIME, length, safe attachment filename,
  `nosniff` and `no-store`. Neither local path nor storage key is returned.
- The JSON text-material flow is unchanged. Student access is not introduced:
  it requires a separate application authorization policy for assigned materials.

## Configuration

- `FILE_STORAGE_DIRECTORY` / `app.file-storage.directory`: default `./var/files`,
  relative to the backend working directory; keep private and writable only by
  the application, outside publicly served directories.
- `MATERIAL_FILES_MAX_SIZE_BYTES` / `app.material-files.max-size-bytes`: 10485760.
  Applies to actual bytes read as well as the multipart file-size limit.
- `MATERIAL_FILES_MAX_REQUEST_SIZE`: 11MB, including multipart overhead; increase
  alongside the file limit if needed.
- `MATERIAL_FILES_ALLOWED_MIME_TYPES`: comma-separated allowlist. FILE accepts
  PDF, PNG/JPEG, ZIP and the educational text formats `.py`, `.sh`, `.js`, `.ts`,
  `.tsx`, `.java`, `.txt`, `.md`, `.json` and `.csv`; IMAGE accepts only PNG/JPEG.
  Extension, declared MIME and content must agree. Text formats may arrive as
  `text/plain` or `application/octet-stream`; `.ts` also accepts the browser-assigned
  `video/mp2t`. Regardless of MIME, text files must be valid UTF-8 without binary
  control characters. PDF, images and ZIP use minimal signatures. New
  formats require a minimal verifier before they can be enabled. These checks are
  not antivirus or full document validation.

Uploads/downloads use bounded in-memory byte arrays for this small-file MVP.
Original names are metadata only; physical objects have server-generated UUID keys.

## Compensation and operational limits

The upload transaction flushes FileAsset and LessonMaterial together. A transaction
completion callback deletes the object on rollback, including DB commit failures.
Local storage also removes partial objects after write failures. Tests exercise
both a position constraint failure and a deferred failure during PostgreSQL commit.

If deletion fails, an ERROR `FILE_STORAGE_CLEANUP_REQUIRED` includes provider and
opaque key. Retain/monitor these logs and retry cleanup after checking that no
`file_assets.storage_key` references the object. An unknown transaction outcome
emits `FILE_STORAGE_RECONCILIATION_REQUIRED`; check the DB before deleting anything.
Do not delete objects merely because their request returned an error.

This synchronous compensation does not provide atomicity across a process crash
between object creation and DB commit. After an abnormal stop, reconcile unreferenced
objects while uploads are stopped. Durable staging/reconciliation is a later step
if crash-safe automatic cleanup becomes a requirement.
