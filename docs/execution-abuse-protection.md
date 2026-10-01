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
