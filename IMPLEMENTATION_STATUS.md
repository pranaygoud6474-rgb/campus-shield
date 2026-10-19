# Implementation Status

Last reviewed: 2026-10-16

## Prioritized checklist

### P0 — Security and data correctness
- [x] Public registration only creates STUDENT accounts; passwords are BCrypt-hashed.
- [x] Use JWT-protected, stateless API access and require a configured JWT secret of at least 32 bytes.
- [x] Keep password hashes and JPA relationships out of API responses by using response DTOs.
- [x] Restrict student incident queries to their own reports; support category, severity, and status filters.
- [x] Attribute audit-log records to the authenticated actor; map controller validation and authorization failures to 4xx API responses.
- [ ] Store the authenticated actor on each incident-history event and expose it in the history response; current actor attribution is available only in the separate admin audit log.
- [x] Provide a one-time, opt-in first-ADMIN bootstrap via local environment variables; password is BCrypt-hashed and repeat startup skips after an administrator exists.
- [x] Reject repeated/backward incident status changes and reopening a CLOSED incident; allow forward progression.

### P1 — Backend workflows
- [x] Incident submission, ownership-scoped listing, assignment, status updates, response notes, history, and database notifications.
- [x] Alerts, notification ownership checks, user profiles, emergency contacts, campus locations, and officer listing APIs.
- [x] Database-derived admin metrics, analytics, user-role management, and audit history.
- [x] Configurable MySQL connection settings and isolated H2 test profile.
- [x] Integration-tested admin bootstrap and officer assignment/status/history/notification workflow using H2.
- [x] Verify MySQL connection and incident persistence against the isolated `campus_safety_verify` database, including a backend restart.
- [x] Cover elevated-role registration denial, allowed/untrusted CORS origins, health-detail suppression, alert enum integration, emergency-contact/alert authorization, notification ownership, audit attribution, and invalid lifecycle transitions in H2 API tests.

### P1 — Frontend integration and browser tests
- [x] Axios API client, session-scoped JWT, visible API errors, login, and student registration.
- [x] Student incident reporting, AI suggestions, tracking, filtering, and history.
- [x] Role-aware admin and officer workflows, including user promotion, incident assignment/status response, response notes, and notification visibility.
- [x] Alerts, profile, notifications, contacts, admin analytics, role management, and audit-log UI are API-backed.
- [x] Added Playwright browser tests for registration/login, rejected login, incident reporting/persistence/history, and all three role workflows.
- [x] Browser verified student denial of admin API access (403), admin role promotion, and officer dashboard/response controls.

### P2 — AI assistance
- [x] Deterministic local keyword-based category/severity suggestions and stored-record category insights are labeled as recommendations.
- [x] Document that `AI_ENABLED`, `AI_PROVIDER`, and `AI_MODEL` are reserved properties and do not make external provider calls.
- [ ] Integrate and test a real external AI provider if credentials and product requirements are provided; local fallback remains active.

### P2 — Runtime and operations
- [x] Installed Temurin OpenJDK 17.0.20.1 at `%USERPROFILE%\.jdk\campus-safety-jdk17\jdk-17.0.20.1+1`.
- [x] Executed Maven integration tests with the Java 17 runtime, not only `--release 17` compilation.
- [x] Started the isolated H2 backend and Vite frontend for Playwright using Java 17; readiness and browser API calls passed.
- [x] Frontend production build and Oxlint passed after E2E additions.
- [x] Start against local MySQL and verify a uniquely generated incident can be read back after a backend restart.
- [x] Verify registration, authenticated incident creation, and retrieval using the MySQL-backed development profile.

## Verification results

| Check | Result |
|---|---|
| `mvn.cmd test` with Temurin 17.0.20.1 in `campus-safety-backend` | **PASS** — 6 tests, 0 failures, 0 errors, 0 skipped; includes unauthenticated/student/officer/admin RBAC matrix; isolated H2 profile; exit code 0 |
| `npm.cmd run build` in `campus-safety-frontend` | **PASS** — Vite production build; exit code 0 |
| `npm.cmd run lint` in `campus-safety-frontend` | **PASS** — Oxlint; exit code 0 |
| `npm.cmd run test:e2e` in `campus-safety-frontend` | **NOT RERUN** — previous 4/4 Chromium result is from 2026-10-14; the current backend occupies port 8080 and Playwright requires exclusive use of that port |
| Current backend readiness | **PASS (existing process)** — listener PID 6356 returned HTTP 200 and `status: UP`; its current health response still includes component details because it predates the latest configuration change |
| Latest-config startup / live MySQL schema validation | **NOT VERIFIED** — avoided restarting the active service or opening another datasource session; tests use isolated H2 |
| First-admin bootstrap | **PASS** — isolated integration test confirms initial admin creation, BCrypt password, idempotent repeat, login, and protected dashboard |
| Local MySQL connectivity / persistence | **PASS** — authenticated student incident creation and retrieval; read-only SQL confirmed the unique record before and after backend restart in `campus_safety_verify` on port 3307 |
| External AI service | **NOT IMPLEMENTED** — no provider request is made; local deterministic recommendations/insights only |

## Configuration and safety notes

- Production/dev backend defaults to the MySQL profile. Configure `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD` locally; `DB_URL` may override the connection. No implicit host/port/schema/user defaults are supplied. Credential values must not be sent in chat or committed.
- The backend requires `JWT_SECRET` of at least 32 UTF-8 bytes.
- First-admin setup is opt-in: set `BOOTSTRAP_ADMIN_ENABLED=true`, `BOOTSTRAP_ADMIN_EMAIL`, and `BOOTSTRAP_ADMIN_PASSWORD` locally during initial setup. The password must be 16–128 characters. It refuses an email collision with a non-admin account, saves a BCrypt hash, and skips if an admin already exists. Public registration remains STUDENT-only.
- The E2E runner uses only an isolated H2 in-memory DB, a test-only admin identity with per-run generated credentials, and a process-local random JWT signing key. Do not use this test setup for development/production.
- Dev/default Hibernate schema mode is `ddl-auto: validate`, which checks mappings without creating/updating tables. `application-test.yml` uses an isolated H2 database and `ddl-auto: create-drop`; never use that test profile with persistent data.
- No Flyway/Liquibase migration tooling or reviewed migration scripts are present. Evolve a persistent schema only through an explicit, reviewed migration; live MySQL schema validation under the new setting has not yet been demonstrated.
- The MySQL verification used the pre-existing `campus_safety_verify` schema and account; the test incident and student account were retained. The verification did not reset, truncate, or delete records.
- Remaining audit gaps: incident-history rows do not store their actor (the separate admin audit log does); analytics aggregate by reading incidents into application memory; invalid JWT error responses use a different envelope; no production deployment/CI manifests exist; external AI is not integrated. Public health details and permissive development CORS were hardened in code, but the currently running process must be restarted for those changes to take effect.
- See [README.md](./README.md) for Windows CMD commands, bootstrap configuration, test instructions, and the external-AI limitation.
