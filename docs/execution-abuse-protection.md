# Student execution abuse protection

Run (`POST /api/v1/student/tasks/{taskId}/run`) and code Submit
(`POST /api/v1/student/tasks/{taskId}/code-submissions`) share an execution
quota keyed by the authenticated student's ID, resolved by the ownership query.
Changing task, homework, topic, session, or IP does not create a new bucket.
The authentication limiter remains separate; text submissions do not consume
execution quota.

Access, context, and executable-task checks run before acquiring quota. A rejected
execution returns HTTP 429 with `EXECUTION_RATE_LIMIT_EXCEEDED`, `Retry-After`
(whole seconds), and `Cache-Control: no-store`. It never calls `ExecutionPort` or
creates a pending submission. Worker failures still consume quota.

| Property | Environment variable | Default |
| --- | --- | --- |
| `execution.abuse-protection.max-source-code-bytes` | `EXECUTION_MAX_SOURCE_CODE_BYTES` | 65536 (64 KiB) |
| `execution.abuse-protection.limit` | `EXECUTION_RATE_LIMIT` | 10 |
| `execution.abuse-protection.window` | `EXECUTION_RATE_WINDOW` | 1m |
| `execution.abuse-protection.max-buckets` | `EXECUTION_RATE_MAX_BUCKETS` | 10000 |
| `execution.abuse-protection.cleanup-interval` | `EXECUTION_RATE_CLEANUP_INTERVAL` | 30s |

Limits and capacity must be positive; durations must be at least 1ms and fit in
nanoseconds. Invalid configuration fails startup.

The synchronized in-memory limiter uses fixed windows measured with a monotonic
clock. Each bucket stores two numbers and a student UUID; expired windows are
removed on reuse, at capacity, and by scheduled cleanup. At capacity, new students
receive 429 until the earliest active window expires. Active buckets are never
evicted, so capacity pressure cannot reset a student's quota.

State is local to one backend process and resets on restart. With multiple
replicas, each replica has its own quota. Fixed windows permit bursts on either
side of a window boundary. This protection does not change worker architecture
or task-specific CPU, memory, and time limits.

Source size is measured on the decoded `sourceCode` string in UTF-8 bytes, not
Java characters or JSON escape bytes. Both services validate it before database
queries, quota acquisition, persistence, and execution. Exactly the configured
size is accepted; a larger source returns HTTP 413 with `SOURCE_CODE_TOO_LARGE`
and a `sourceCode` error detail. The configurable range is 1 byte to 1 MiB.
Existing blank-source validation still applies.

This application validation runs after JSON deserialization. The ingress proxy
should also cap the total HTTP body to bound JSON parsing, whitespace, and other
request fields. The source boundary does not change task execution limits.
