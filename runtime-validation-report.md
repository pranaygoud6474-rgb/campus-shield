# Runtime Validation Report

**Generated**: 2026-10-16T14:40:00+05:30
**Target**: `C:\AI-Powered Campus Safety-1`

## Summary

| Step | Status | Exit Code | Details |
|---|---|---:|---|
| Java 17 backend integration | PASS | 0 | `mvn.cmd test`; 6 tests, all passed including unauthenticated/student/officer/admin RBAC matrix; Temurin 17.0.20.1; isolated H2 test profile |
| Current backend health | PASS (existing process) | 0 | Port 8080 listener PID 6356; `GET /actuator/health` returned HTTP 200 and overall/database `UP`; response still exposed components because process predates latest configuration |
| Frontend build | PASS | 0 | `npm.cmd run build`; Vite production build succeeded |
| Frontend lint | PASS | 0 | `npm.cmd run lint`; Oxlint succeeded |
| Browser E2E | NOT RERUN | n/a | Previously 4/4 Chromium tests passed on 2026-10-14; current backend owns port 8080 and Playwright config requires an exclusive listener |
| MySQL persistence | PASS (prior evidence; not repeated) | n/a | Authenticated student registration, incident creation and retrieval; read-only MySQL query confirmed the uniquely marked incident before and after backend restart in `campus_safety_verify` |
| Latest-config MySQL startup/schema validation | UNVERIFIED | n/a | No restart or second database-backed process was launched; preserve active service and avoid an unrequested DB reconnect |

**Overall**: NEEDS SIGN-OFF — source changes pass all 5 backend tests and frontend lint/build; earlier browser and MySQL persistence evidence remains valid. Latest dev configuration has not been applied to the active process or checked against MySQL, and browser E2E was not rerun due the occupied exclusive port.

## Environment and capability decisions

- **Docker**: UNAVAILABLE — `docker info` failed because `docker` was not recognized.
- **Node.js**: AVAILABLE — v24.19.0.
- **Playwright**: AVAILABLE — `@playwright/test` 1.64.1; Chromium was installed in the prior E2E run.
- **Database/infrastructure tier for automated tests**: FALLBACK (embedded/in-memory H2) — Docker is unavailable. H2 tests do not establish MySQL dialect/schema compatibility. Separate live MySQL connectivity and persistence across restart were previously verified.
- **Browser tier**: AVAILABLE, NOT RERUN — Node.js and Playwright are available; execution was withheld because the current backend owns port 8080 and the test server has `reuseExistingServer: false`.
- E2E Playwright server reuse is disabled, and the backend launcher explicitly selects the `test` profile/H2. The test admin password and JWT signing key are generated per run; tests do not connect to the user's MySQL database.

## Journeys covered

1. Register a student, sign out, log in, and confirm the stored account remains usable.
2. Reject invalid credentials, then register/login successfully.
3. Verify unauthenticated incident reads return 401; register, request local AI recommendation, submit an incident, sign in again, and read its history.
4. Verify a student is denied the admin API (403); admin promotes that user to SECURITY_OFFICER; officer assigns an incident, changes status, adds a note, views history, and the reporter receives a status notification.
5. Backend integration coverage validates ADMIN and SECURITY_OFFICER registration denial, profile update/login, health-detail suppression, exact allowed/rejected CORS origins, first-admin bootstrap/hash/idempotence, incident ownership and lifecycle, audit attribution, cross-user notification denial, WEATHER alert support and role restrictions, emergency-contact write restrictions, dashboard analytics, and endpoint authorization for all three roles.

## RBAC endpoint matrix

The table records the expected and observed status codes asserted by the passing `mvn.cmd test` integration suite. Tests use random-port Spring Boot integration tests and the test profile's isolated H2 database; they do not connect to MySQL.

| Principal | Endpoint(s) | Expected | Observed |
|---|---|---:|---:|
| Anonymous | `GET /api/v1/auth/me`, `GET /api/v1/incidents`, `GET /api/v1/officers`, `GET /api/v1/admin/dashboard`, `GET /api/v1/ai/safety-insights`, `POST /api/v1/alerts`, `PATCH /api/v1/incidents/{id}/assign`, `PATCH /api/v1/incidents/{id}/status` | 401 | 401 |
| Student | `GET /api/v1/incidents` (own report); `POST /api/v1/ai/classify-incident` | 200 | 200 |
| Student | `GET /api/v1/admin/{dashboard,analytics,users,audit-logs}`, `GET /api/v1/ai/safety-insights`, `GET /api/v1/officers`, `PATCH /api/v1/admin/users/{id}/role`, `PATCH /api/v1/incidents/{id}/{assign,status}`, `POST /api/v1/incidents/{id}/notes`, `POST /api/v1/locations`, `POST /api/v1/emergency-contacts`, `POST /api/v1/alerts`, `POST /api/v1/ai/summarize-incident` | 403 | 403 |
| Security officer | `GET /api/v1/officers`, `POST /api/v1/locations`, `PATCH /api/v1/incidents/{id}/assign`, `PATCH /api/v1/incidents/{id}/status`, `POST /api/v1/incidents/{id}/notes`, `POST /api/v1/ai/summarize-incident` | 200 | 200 |
| Security officer | `GET /api/v1/admin/{dashboard,analytics,users,audit-logs}`, `GET /api/v1/ai/safety-insights`, `PATCH /api/v1/admin/users/{id}/role`, `POST/PATCH/DELETE /api/v1/alerts`, `POST /api/v1/emergency-contacts` | 403 | 403 |
| Administrator | `GET /api/v1/admin/{dashboard,analytics,users,audit-logs}`, `PATCH /api/v1/admin/users/{id}/role`, `GET /api/v1/ai/safety-insights`, `POST/PATCH/DELETE /api/v1/alerts`, `POST /api/v1/emergency-contacts`, `POST /api/v1/locations`, `PATCH /api/v1/incidents/{id}/status`, `POST /api/v1/incidents/{id}/notes`, `POST /api/v1/ai/summarize-incident` | 200 (DELETE alert 204) | 200 (DELETE alert 204) |
| Public registration | `POST /api/v1/auth/register` with `role=ADMIN` or `role=SECURITY_OFFICER` | 400 | 400 |

## Legacy test assets

- Discovered Java integration tests in `campus-safety-backend/src/test`; retained and extended.
- No frontend browser test suite was present. Added Playwright specs under `campus-safety-frontend/e2e/`.
- No legacy browser framework migration was required.

## Evidence

- Maven Surefire reports: `campus-safety-backend/target/surefire-reports/`.
- Playwright HTML report: `campus-safety-frontend/playwright-report/index.html`.
- Playwright run artifacts: `campus-safety-frontend/test-results/`.
- Environment prerequisites and versions were checked locally; no database credential values were read or printed.

## Focused final audit

- **Authentication and student data**: Public registration is restricted to STUDENT; tests exercise and reject both ADMIN and SECURITY_OFFICER registration attempts. BCrypt/JWT are configured; authenticated profile update/login and password-hash omission are tested. Student incident ownership is covered. Emergency-contact reads are public; creation requires ADMIN and is tested.
- **Incident response and authorization**: Student status changes are denied; officer/admin workflows are covered. Repeated/backward transitions and reopening CLOSED are rejected; forward closing is permitted. The admin audit-log API confirms student/admin/officer actor attribution. Incident-history rows themselves still do not store or return an actor.
- **Alerts and notifications**: Active alerts are public-read; writes require ADMIN. WEATHER (and SECURITY) alert types now align with frontend options. Tests reject student alert creation and cross-user notification modification. Incident notifications are user-scoped.
- **Analytics and errors**: Dashboard and analytics are ADMIN-only and derive counts from persisted records. The API exception advice maps validation and authorization conditions to 4xx responses. Analytics currently load all incidents into application memory for aggregation, which may not scale to large datasets.
- **Error-envelope gap**: Invalid/expired bearer tokens are answered directly by the JWT filter using `sendError`; they do not pass through the JSON API exception advice, so error shape may differ from other API failures.
- **Frontend integration**: The Axios client defaults to `http://localhost:8080/api/v1`, stores JWT only in session storage, and renders API errors. Oxlint and Vite production build passed. Earlier E2E result is 4/4; it was not rerun because the live backend occupies its required port.
- **Security and deployment**: Source config now hides health details and permits only the exact configured `FRONTEND_URL`; H2 integration tests cover both. The active process has not been restarted and its current health response still exposes component details, so the hardening is not live. Dev/default Hibernate is now `ddl-auto:validate`, which does not alter tables. No migration framework or production deployment/CI manifests are present.

## Known gaps / follow-up

| # | Severity | Description | Escalated To |
|---|---|---|---|
| 1 | MEDIUM | Health-detail suppression and schema-validation config are not active on running PID 6356; current health response still exposes component details including database and host path information. Restart intentionally using the documented CMD procedure before relying on source hardening. | Local operator |
| 2 | MEDIUM | Incident-history rows do not include the authenticated actor; actor attribution is available in the separate admin audit log. | Schema/API enhancement |
| 3 | MEDIUM | Dashboard/category analytics aggregate using full-table reads in application memory; use database-side aggregation for larger deployments. | Scalability work |
| 4 | MEDIUM | `ddl-auto:validate` has not been exercised against the live MySQL schema; no migration framework or reviewed migrations exist. | Local operator / schema process |
| 5 | MEDIUM | No deployment manifests or CI pipeline are present; production deployment readiness is not established. | Operations |
| 6 | LOW | External AI provider is not integrated. `AI_ENABLED`, `AI_PROVIDER`, and `AI_MODEL` remain reserved; only deterministic local recommendations run. | Product/provider requirements |
| 7 | LOW | Invalid/expired bearer-token failures originate in the JWT filter and may not use the same JSON error envelope as controller validation errors. | API consistency |

## Runtime verdict

```text
environment:
  docker: UNAVAILABLE — `docker info` failed because docker is not recognized
  node: AVAILABLE — v24.19.0
  playwright: AVAILABLE — @playwright/test 1.64.1; Chromium installed during prior E2E run
  infra-tier: FALLBACK(embedded/in-memory) — Docker unavailable; backend tests use H2
  browser-tier: SKIPPED — active backend PID 6356 owns port 8080; Playwright config requires an exclusive listener
startup: PARTIAL — existing process returned HTTP 200 from /actuator/health with overall/database UP; latest config not restarted
integration: PASS — exit_code: 0, passed: 5, failed: 0, skipped: 0; isolated H2 integration tests
e2e: UNVERIFIED — exit_code: n/a, passed: n/a, failed: n/a; previous run 4/4, not rerun because required port is occupied
overall: NEEDS_SIGNOFF — source-level checks pass; latest config/live MySQL validation and a fresh browser run remain unverified
```
