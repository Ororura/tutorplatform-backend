# Backend health probes

Spring Boot Actuator serves probes on the application port (8080 by default).
Infrastructure can call these GET endpoints without a session or CSRF token:

| URL (default local address) | Semantics |
| --- | --- |
| `http://localhost:8080/actuator/health/liveness` | `livenessState` only: the application is alive. PostgreSQL and execution worker failures do not change this probe. Use it for restart decisions. |
| `http://localhost:8080/actuator/health/readiness` | `readinessState` and `db`: the application accepts traffic and PostgreSQL is available. Use it for traffic admission and container health. |
| `http://localhost:8080/actuator/health` | Aggregate health retained for existing monitoring; do not use it for restart decisions. |
| `http://localhost:8080/actuator/prometheus` | Existing Prometheus scrape endpoint; remains available without authentication. |

Healthy probes return HTTP 200 with `status: UP`; failed readiness returns HTTP
503 (`DOWN` for database failure, `OUT_OF_SERVICE` when refusing traffic).
Health responses hide component names and diagnostic details, including for
authenticated users. All other Actuator URLs, including discovery and health
component paths, are denied by security. Only health and Prometheus endpoints
have Actuator access enabled.

PostgreSQL is mandatory because backend domain data and HTTP sessions use it.
The execution worker is deliberately excluded from readiness: it handles only
code execution, and `HttpExecutionAdapter` already translates worker failures
into `SYSTEM_ERROR` execution results. A temporary worker outage should not
remove students, materials, reports, and other backend functionality from
service. Monitor execution failures separately.

The backend Docker image checks readiness on `127.0.0.1:8080`. Compose inherits
the image healthcheck unless it supplies an override; the existing frontend E2E
Compose override also uses readiness. Docker marks the container unhealthy on
probe failure; a healthcheck alone does not restart it or remove traffic. An
orchestrator must implement those policies. Allow startup time for Flyway and
application initialization. Keep infrastructure access to health and Prometheus
in the reverse proxy/network rules; no dedicated Prometheus scrape configuration
is tracked in this backend repository.
