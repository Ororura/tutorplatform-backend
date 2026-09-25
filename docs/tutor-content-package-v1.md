# Tutor Content Package v1 Specification

## 1. Purpose and scope

Tutor Content Package v1 is a portable, single-document UTF-8 YAML format for adding modules, topics, and lesson materials to an **existing** `LearningProgram`. The target program is selected outside the YAML package. Import creates ordinary `Module`, `Topic`, and `LessonMaterial` records; it does not create another program or a parallel content model.

This document specifies the package and the behavior a future preview/import workflow must provide. It does not define implemented endpoints. The existing teacher APIs and OpenAPI contract remain unchanged at this milestone.

## 2. Complete YAML shape

```yaml
schemaVersion: 1
kind: modules
modules:
  - title: "Module title"
    description: "Optional module description"
    topics:
      - title: "Topic title"
        description: "Optional topic description"
        materials:
          - title: "Theory"
            materialType: MARKDOWN
            content: |
              Non-empty Markdown content.
          - title: "Reference"
            materialType: LINK
            externalUrl: "https://example.org/reference"
```

`description` may be omitted. `materials` may be omitted or `[]`. A topic has no other fields in v1. The same module and topic shapes repeat in their arrays; the material shape depends on `materialType`.

### Root fields

| Field | Type | Required | Rule |
| --- | --- | --- | --- |
| `schemaVersion` | integer | Yes | Exactly `1`; other versions are rejected. |
| `kind` | string | Yes | Exactly `modules`. |
| `modules` | array of module objects | Yes | 1–15 entries. |

### Module fields

| Field | Type | Required | Rule |
| --- | --- | --- | --- |
| `title` | string | Yes | Non-blank after trimming; at most 180 characters. |
| `description` | string | No | Optional plain string; no separate backend length limit. |
| `topics` | array of topic objects | Yes | 1–25 entries per module. |

### Topic fields

| Field | Type | Required | Rule |
| --- | --- | --- | --- |
| `title` | string | Yes | Non-blank after trimming; at most 180 characters. |
| `description` | string | No | Optional plain string; no separate backend length limit. |
| `materials` | array of material objects | No | May be absent or empty; at most 250 materials across the entire package. |

### Material fields

| Field | Type | Required | Rule |
| --- | --- | --- | --- |
| `title` | string | Yes | Non-blank; at most 200 characters. |
| `materialType` | string enum | Yes | One of the four v1 values below. |
| `content` | string | For `TEXT`, `MARKDOWN`, `CODE_EXAMPLE` | Non-blank; forbidden for `LINK`. No separate backend length limit. |
| `externalUrl` | string | For `LINK` | Non-blank absolute HTTP or HTTPS URL; forbidden for text materials. No separate backend length limit. |

## 3. Supported material types

| `materialType` | Payload | Meaning |
| --- | --- | --- |
| `TEXT` | `content` | Plain text. |
| `MARKDOWN` | `content` | Markdown text. |
| `CODE_EXAMPLE` | `content` | Code example as text; not executed by import. |
| `LINK` | `externalUrl` | External HTTP/HTTPS resource. |

`IMAGE` and `FILE` exist in the backend material enum but are **reserved** for a future ZIP import format. They are invalid in a v1 YAML package. `fileAssetId`, binary content, and storage keys are not package fields; v1 does not use `FileStorage`.

## 4. Validation and limits

- Accept exactly one YAML document whose root is a mapping with only `schemaVersion`, `kind`, and `modules`. Every nested mapping may contain only the fields in its table. Reject unknown fields, duplicate keys, unknown material types, and unknown schema versions rather than silently ignoring them.
- `modules` and every `topics` value must be arrays with the stated minimums. `materials`, when present, must be an array. Count all materials in all topics against the package limit.
- The UTF-8 encoded YAML file is at most **1 MiB (1,048,576 bytes)**. Reject an oversize file before unbounded buffering or parsing. This bound also limits the combined size of descriptions, content, and URLs, for which the current backend has no independent maximum.
- Titles must pass the existing create constraints: module/topic titles are non-blank and at most **180 characters**; material titles are non-blank and at most **200 characters**. Match the backend's title length semantics (`String.length()` after any applicable normalization) rather than inventing a different character count. The module/topic create DTOs strip titles and descriptions; preview and import must agree on the values to be stored.
- `content` for text types must be present and non-blank. For `LINK`, `externalUrl` must be present and non-blank, parse as an absolute URL with a host, and use only the `http` or `https` scheme. Reject all other schemes and malformed URLs. The current material service checks presence and type exclusivity; the HTTP/HTTPS restriction is an additional package-v1 rule.
- A text material must not contain `externalUrl`; a `LINK` must not contain `content`. `position`, UUIDs, slugs, status, teacher IDs, program IDs, file asset IDs, and version fields are not accepted from the package.
- Array order defines display order. Append imported modules after existing modules in the selected program. Within each new module and topic, assign positions in the order of their `topics` and `materials` arrays. Platform mechanisms create UUIDs and slugs; package authors cannot supply them.

## 5. Security requirements

- Parse YAML as data only: do not instantiate or execute arbitrary YAML tags or application classes. Reject explicit tags in v1.
- Reject anchors and aliases in v1; do not expand them. Reject recursive or excessively nested structures. The only allowed nesting is the shape shown above (root → `modules` → module → `topics` → topic → `materials` → material), with scalar values at the leaves.
- Enforce the byte limit before parsing, then enforce collection counts and nesting while parsing so hostile input cannot exhaust memory or stack space. Reject multiple documents.
- Detect duplicate mapping keys, including at nested levels, before mapping to package fields.
- Decode strictly as UTF-8. Reject malformed byte sequences and invalid encodings safely with a validation error; do not replace invalid bytes silently.
- Import v1 is create-only for module/topic/material records. The package cannot identify, update, delete, replace, or move existing entities. Apply existing teacher authentication, program ownership, program editability, and topic/material ownership rules. In particular, the current program service disallows editing archived or student-assigned programs.

## 6. Preview and atomic import

1. Preview validates the full package and the selected target's authorization/editability and reports the proposed hierarchy, order, counts, and validation errors. It is read-only and makes **no database changes**.
2. Confirmation is bound to the selected program and the exact package bytes or digest previewed. Import **revalidates the file**, including format, limits, ownership, and current target editability; preview alone does not authorize a later import. If the file or target context has changed, reject or require a new preview.
3. Create the new modules, topics, and materials in **one database transaction**. Preserve array order and use platform-generated identifiers and slugs. A validation, authorization, constraint, or persistence failure rolls back the entire structure; no partial import remains.
4. A confirmation has a stable idempotency identity scoped to the teacher, target program, and package. Check and record it atomically with creation. Retrying the **same confirmation** returns its previous outcome without creating duplicate records; a separate confirmation may intentionally append a second copy. Concurrent retries must obey the same guarantee.
5. v1 performs no file writes, so no `FileStorage` compensation is involved. Any eventual API should document preview/confirmation requests and errors in OpenAPI and follow the existing teacher session and CSRF conventions.

## 7. Complete example

The committed [Python conditions example](examples/python-conditions.yaml) is a valid v1 package with one module, two topics, Markdown theory, and code examples:

```yaml
schemaVersion: 1
kind: modules
modules:
  - title: "Условные конструкции в Python"
    description: "Ветвление программы с помощью if, elif и else."
    topics:
      - title: "Условие if"
        description: "Проверка одного условия."
        materials:
          - title: "Теория: if"
            materialType: MARKDOWN
            content: |
              `if` выполняет блок кода, когда условие истинно.
          - title: "Пример: if"
            materialType: CODE_EXAMPLE
            content: |
              temperature = 25
              if temperature > 20:
                  print("Тепло")
      - title: "Ветвление elif и else"
        description: "Выбор одной из нескольких ветвей."
        materials:
          - title: "Теория: elif и else"
            materialType: MARKDOWN
            content: |
              `elif` проверяет следующее условие, а `else` обрабатывает остальные случаи.
          - title: "Пример: elif и else"
            materialType: CODE_EXAMPLE
            content: |
              score = 75
              if score >= 90:
                  print("Отлично")
              elif score >= 60:
                  print("Зачёт")
              else:
                  print("Повторить тему")
```

## 8. Outside v1

- Creating or editing `LearningProgram` itself, changing its subject/status, or assigning it to students.
- Updating, deleting, replacing, or linking to existing modules, topics, or materials.
- Images, files, ZIP archives, attachments, `FileStorage` objects, and remote-resource fetching.
- Tasks, homework, assessments, progress, sessions, and executable code evaluation.
- User-supplied identifiers, slugs, positions, ownership, or metadata beyond the fields listed above.

## 9. Path to v2

Introduce `schemaVersion: 2` as a separately validated contract; do not reinterpret v1 files. A v2 proposal can use a ZIP manifest plus assets for `IMAGE` and `FILE`, with explicit archive size/count/path limits, MIME and content checks, and `FileStorage` transaction/cleanup design. Define any additional metadata or references as versioned fields, document migration/compatibility in OpenAPI, and keep v1 create-only behavior stable for existing packages.
