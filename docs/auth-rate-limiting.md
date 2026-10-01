# Authentication rate limiting

The backend limits these POST endpoints before controller dispatch, after CSRF validation:

| Operation | Endpoint | Default quota per client address |
| --- | --- | --- |
| Login | `/api/v1/auth/login` | 10 requests / 1 minute |
| Teacher registration | `/api/v1/auth/register/teacher` | 5 requests / 10 minutes |
| Student invitation acceptance | `/api/v1/public/student-invitations/{token}/accept` | 10 requests / 10 minutes |
| Teacher invitation acceptance | `/api/v1/public/teacher-invitations/{token}/accept` | 10 requests / 10 minutes |

Each operation has a separate bucket for the servlet client address (`request.getRemoteAddr()`).
All CSRF-valid requests count, including successful requests, invalid credentials, invalid JSON,
validation failures and unknown invitation tokens. Changing an email, invitation token or
session does not reset the quota. Teacher registration is limited in both OPEN and INVITE_ONLY
modes; below the quota the existing registration policy still applies. GET metadata, CSRF,
logout, current-user requests and authenticated CRUD are outside this limiter.

Unknown email and wrong password still return the same `401 AUTH_INVALID_CREDENTIALS` error
below the quota and the same `429 RATE_LIMIT_EXCEEDED` error above it. The limiter never reads
request bodies and stores no email, password, invitation token or session secret.

## Configuration

Properties use the prefix `app.security.auth-rate-limit`. Durations accept Spring Boot
syntax such as `30s`, `1m`, `10m`, or ISO-8601 durations. All quotas and the bucket capacity
must be positive; windows and cleanup interval must be at least 1 millisecond. Invalid
configuration fails startup.

| Property suffix | Environment variable | Default |
| --- | --- | --- |
| `login-limit` | `AUTH_RATE_LIMIT_LOGIN_LIMIT` | `10` |
| `login-window` | `AUTH_RATE_LIMIT_LOGIN_WINDOW` | `1m` |
| `registration-limit` | `AUTH_RATE_LIMIT_REGISTRATION_LIMIT` | `5` |
| `registration-window` | `AUTH_RATE_LIMIT_REGISTRATION_WINDOW` | `10m` |
| `student-invitation-limit` | `AUTH_RATE_LIMIT_STUDENT_INVITATION_LIMIT` | `10` |
| `teacher-invitation-limit` | `AUTH_RATE_LIMIT_TEACHER_INVITATION_LIMIT` | `10` |
| `invitation-window` | `AUTH_RATE_LIMIT_INVITATION_WINDOW` | `10m` |
| `max-buckets` | `AUTH_RATE_LIMIT_MAX_BUCKETS` | `10000` |
| `cleanup-interval` | `AUTH_RATE_LIMIT_CLEANUP_INTERVAL` | `30s` |

Environment variables must reach the backend container/process; setting a Compose `.env`
value alone does not inject a variable unless the deployment passes it to the backend.

## Client addresses and reverse proxies

The current application configuration and Compose deployment do not define a trusted
reverse proxy. The frontend uses Next.js API rewrites. Forwarded headers are explicitly
ignored by default (`server.forward-headers-strategy=NONE`, configurable with
`SERVER_FORWARD_HEADERS_STRATEGY`). Consequently, requests relayed through Next.js or
another proxy share that proxy's address and quota. Users sharing a NAT address also share
quotas. Direct clients cannot evade the limiter with arbitrary `X-Forwarded-For`,
`Forwarded`, or `X-Real-IP` headers.

For a deployment with known proxy addresses, use Tomcat's native address resolution and
explicitly restrict which proxy peers it trusts. For example, **only if the actual proxy
peer is `10.20.0.5`**, pass these variables to the backend:

```dotenv
SERVER_FORWARD_HEADERS_STRATEGY=NATIVE
SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES=10[.]20[.]0[.]5
```

Restrict backend network access to the intended proxies. Configure the outermost proxy
to remove client-supplied forwarded headers and write the actual client address. For
multiple proxy hops, configure the complete known chain, including any Next.js hop, and
verify that `remoteAddr` resolves to the real client. Do not enable blanket FRAMEWORK
header trust or leave the broad default internal-proxy trust expression when enabling
NATIVE. Never use an empty or wildcard trust expression in production.

See [Spring Boot's reverse-proxy configuration guidance](https://docs.spring.io/spring-boot/how-to/webserver.html#howto.webserver.use-behind-a-proxy-server).
The limiter itself does not parse forwarded headers; it uses the address resolved by the
configured servlet container.

## Response and operational limits

Exceeded quotas return HTTP 429 with JSON in the existing `ApiError` format:

```json
{
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "Too many authentication requests. Try again later.",
  "timestamp": "2026-10-01T12:00:00Z",
  "traceId": "example-trace-id",
  "details": []
}
```

`Retry-After` contains the remaining window in seconds, rounded up to at least one.
Responses use `Cache-Control: no-store`. CSRF failures retain HTTP 403 and do not consume
quota. Rejected requests do not reach password hashing, account creation or invitation
acceptance.

This implementation uses synchronized in-memory fixed windows and a monotonic ticker.
Expired buckets are removed periodically, when reused, and before rejecting a new bucket
at capacity. The map never exceeds `max-buckets`. If capacity contains only active
buckets, new addresses/operations receive 429 until the earliest bucket expires; existing
buckets retain their quota. This deliberately avoids evicting active restrictions. IP
churn can therefore temporarily deny new auth clients at capacity.

Limits are local to one backend instance and reset on restart. Fixed windows allow a
burst near a boundary. Before horizontal scaling, use coordinated limits or equivalent
trusted edge protection. No Redis or other dependency is added for the single-instance MVP.

## Verification

Run `./gradlew build`. Unit tests cover configuration binding, invalid settings, exact
window expiry, retry rounding, operation/address isolation, concurrent requests and
bounded memory/cleanup. PostgreSQL + MockMvc integration tests exercise the real security
chain and auth controllers with deterministic low quotas and a controllable monotonic
ticker. Existing business integration suites use generous quotas because all their
independent scenarios share MockMvc's default peer; limiting remains enabled.
