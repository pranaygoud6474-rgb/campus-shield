# AI-Powered Campus Safety and Emergency Management System

Existing project maintained as a React/Vite frontend and a Java 17 / Spring Boot / Maven backend. The backend uses Spring Data JPA and MySQL for development; its integration tests use an isolated in-memory H2 database.

## Current functionality

- Student registration and login with BCrypt password hashing and JWT authentication.
- Role-aware student, security officer, and administrator API/UI workflows. Public registration creates STUDENT users only.
- Incident reporting and tracking, filtering, assignment/status response, history, and audit attribution.
- Database-backed alerts and notifications; profile and emergency-contact management.
- Database-derived administration metrics and analytics.
- Local rule-based incident category/severity suggestions and history summaries. All suggestions require human review; no external AI provider is currently connected.

For verified work and known gaps, see [IMPLEMENTATION_STATUS.md](./IMPLEMENTATION_STATUS.md).

## Prerequisites

- MySQL Server and the MySQL command-line client (`mysql.exe`).
- Java 17 (the Maven project targets Java 17).
- Maven 3.9 or later.
- Node.js and npm.

The local environment used during this verification had MySQL listening on port `3307`; change `DB_PORT` below if yours uses another port. MySQL credentials are intentionally read only from local environment/configuration and must not be pasted into chat or committed.

## Render deployment

The backend container defaults to the `dev` Spring profile. Its datasource settings are defined centrally in `application.yml`; `application-dev.yml` only sets the development schema-validation mode. For Render with an Aiven MySQL service, configure these Web Service environment variables in the Render dashboard:

| Variable | Value / format |
|---|---|
| `DB_HOST` | Aiven service hostname only (no scheme or port) |
| `DB_PORT` | Aiven service port, for example `18082` |
| `DB_NAME` | Database/schema name, for example `defaultdb` |
| `DB_USERNAME` | Database username, for example `avnadmin` |
| `DB_PASSWORD` | Aiven password; enter only in Render's secret environment-variable field |
| `DB_SSL_MODE` | `REQUIRED` to require encrypted MySQL transport |
| `JWT_SECRET` | Random secret with at least 32 UTF-8 bytes |
| `BOOTSTRAP_ADMIN_ENABLED` | `false` unless deliberately enabling a one-time initial administrator |
| `FRONTEND_URL` | Exact deployed frontend origin, including scheme and hostname |

`DB_URL` is optional and, when supplied, takes precedence over the host/port/name variables; use a complete Connector/J URL with its TLS options. Do not set `DB_URL` and expect `DB_SSL_MODE` to modify it. The Dockerfile uses Render's automatically supplied `PORT` (with `10000` as the local/container fallback), so no `SERVER_PORT` override is required. The MySQL driver is included in the packaged application; no local properties file is required in the image.

For the Vercel frontend, set `FRONTEND_URL` to `https://campus-shield-pi.vercel.app` exactly (origin only; no API path or trailing slash). For stronger server identity validation than encryption-only `REQUIRED`, configure Connector/J with `sslMode=VERIFY_IDENTITY` and supply/configure the Aiven CA certificate as documented by Aiven. Never put the certificate or database credentials in the repository or Docker image.

## Verify MySQL access (read-only)

The isolated local verification database is `campus_safety_verify` on port `3307`. Inspect the existing account and schema with the MySQL client; `-p` prompts locally and does not put the password in the command:

```cmd
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -h 127.0.0.1 -P 3307 -u campus_user -p -D campus_safety_verify -e "SELECT DATABASE(); SHOW GRANTS; SHOW TABLES;"
```

Do not recreate the account, change its password, or run destructive SQL as a startup troubleshooting step. The application requires explicit DB settings at runtime and Hibernate now uses `ddl-auto=validate` outside the isolated test profile: startup checks schema compatibility but does not create/update tables. No Flyway/Liquibase migration set is currently included; schema changes require a reviewed, explicit migration before deploying a changed model.

## Create the first administrator

Public registration can create STUDENT accounts only. To create the first ADMIN, opt in on the first application startup by setting `BOOTSTRAP_ADMIN_ENABLED=true`, `BOOTSTRAP_ADMIN_EMAIL`, and `BOOTSTRAP_ADMIN_PASSWORD` in the local process environment. Use a unique password of 16–128 characters and do not put the secret in source control or chat.

```cmd
set BOOTSTRAP_ADMIN_ENABLED=true
set BOOTSTRAP_ADMIN_EMAIL=admin@example.edu
set BOOTSTRAP_ADMIN_PASSWORD=use-a-locally-configured-secret
```

The backend normalizes the email, BCrypt-hashes the password, and refuses a collision with an existing non-admin account. Once an ADMIN exists, subsequent bootstraps skip without modifying that account. Disable/unset the bootstrap variables after initial setup. In the sample frontend E2E environment, the only bootstrap account is a fixed test-only identity in the isolated in-memory H2 database.

## Start the applications in separate Windows CMD terminals

### Terminal 1 — Spring Boot backend

First check whether port 8080 is already occupied. If the output contains `LISTENING`, do not launch a second backend; check `http://localhost:8080/actuator/health` or stop/restart the known process intentionally.

```cmd
netstat -ano | findstr /R /C:":8080 .*LISTENING"
```

From a fresh CMD terminal, configure the local database values, verify Java 17, and launch from the directory containing `pom.xml`. The Node.js prompt below disables terminal echo for the password; the password is supplied only in the child process environment. A cryptographically random 32-byte JWT secret is generated in memory for this run. Do not type credentials into a command or commit them.

```cmd
cd /d "C:\AI-Powered Campus Safety-1\campus-safety-backend"
set "JAVA_HOME=%USERPROFILE%\.jdk\campus-safety-jdk17\jdk-17.0.20.1+1"
set "PATH=%JAVA_HOME%\bin;%PATH%"
java -version
set "DB_URL="
set "APP_PROFILE=dev"
set "DB_HOST=127.0.0.1"
set "DB_PORT=3307"
set "DB_NAME=campus_safety_verify"
set "DB_USERNAME=campus_user"
set "FRONTEND_URL=http://localhost:5173"
set "SERVER_PORT=8080"
set "BOOTSTRAP_ADMIN_ENABLED=false"
node -e "const readline=require('readline'),crypto=require('crypto'),{spawn}=require('child_process');const rl=readline.createInterface({input:process.stdin,output:process.stdout,terminal:true});rl._writeToOutput=()=>{};process.stdout.write('MySQL password (input hidden): ');rl.question('',value=>{rl.close();const env={...process.env};env.DB_PASSWORD=value;env.JWT_SECRET=crypto.randomBytes(32).toString('base64');value='';const child=spawn('mvn.cmd',['spring-boot:run'],{cwd:process.cwd(),env,stdio:'inherit',shell:true});child.on('error',error=>{console.error(error.message);process.exitCode=1});child.on('exit',code=>{env.DB_PASSWORD='';env.JWT_SECRET='';process.exitCode=code===null?1:code})});"
```

For this machine, Temurin 17.0.20.1 is under `%USERPROFILE%\.jdk\campus-safety-jdk17`; `java -version` must report version 17. The backend API base URL is `http://localhost:8080/api/v1`; health is `http://localhost:8080/actuator/health`. Health returns status only; datasource details are not exposed publicly. The backend CORS origin defaults to the exact `http://localhost:5173`; set `FRONTEND_URL` to the exact deployed frontend origin in another environment.

### Terminal 2 — React/Vite frontend

```cmd
cd /d "C:\AI-Powered Campus Safety-1\campus-safety-frontend"
npm.cmd install
npm.cmd run dev -- --host 127.0.0.1
```

Open `http://localhost:5173`. During local development, the frontend defaults to `http://localhost:8080/api/v1` if `VITE_API_BASE_URL` is unset. Production builds require `VITE_API_BASE_URL` and do not fall back to localhost. For the Vercel deployment, set it to `https://campus-shield-backend-2gp1.onrender.com/api/v1` in the Vercel project settings, then redeploy so Vite embeds the value at build time.

```cmd
set VITE_API_BASE_URL=http://localhost:8080/api/v1
npm.cmd run dev -- --host 127.0.0.1
```

## Build and test

Backend integration tests use the test profile and in-memory H2; they do not validate MySQL credentials or modify the MySQL database:

```cmd
cd /d "C:\AI-Powered Campus Safety-1\campus-safety-backend"
mvn.cmd test
```

Build and lint the frontend:

```cmd
cd /d "C:\AI-Powered Campus Safety-1\campus-safety-frontend"
npm.cmd run build
npm.cmd run lint
```

Run the automated browser journeys (Playwright starts a Java 17 test backend using H2, plus Vite):

```cmd
cd /d "C:\AI-Powered Campus Safety-1\campus-safety-frontend"
npm.cmd run test:e2e
```

Latest backend RBAC validation: `mvn.cmd test` passed 6/6 integration tests on Temurin Java 17.0.20.1 (exit code 0), including a fresh endpoint authorization matrix using isolated H2; frontend `npm.cmd run lint` and `npm.cmd run build` previously passed (exit code 0). Playwright previously passed 4/4 Chromium journeys (recorded 2026-10-14); it was not rerun because the current backend occupies port 8080 and the E2E config requires an exclusive backend on that port. See the [runtime validation report](./runtime-validation-report.md) for endpoint-level RBAC evidence and gaps.

MySQL persistence was verified on 2026-10-16 against the isolated `campus_safety_verify` database on port 3307. Using the normal student registration/JWT flow, a uniquely marked low-severity test incident was created and retrieved through the API. A read-only MySQL query confirmed the record before and after a backend restart. The test student and incident were retained; no rows were deleted. The live process currently on port 8080 predates the latest configuration hardening; restart it intentionally to load the validation-only schema setting and health-detail suppression. See [runtime-validation-report.md](./runtime-validation-report.md) for the evidence and remaining gaps.
## Important limitations

- No external AI provider is currently integrated. `AI_ENABLED`, `AI_PROVIDER`, and `AI_MODEL` are reserved configuration properties and do not cause external requests; the current classifier/summary is deterministic local fallback logic. Do not treat its outputs as a substitute for human judgement or as incident statistics.
- E2E tests use a test-only admin identity with per-run generated credentials, a process-local random JWT signing key, and a disposable in-memory H2 store. Never use these settings with the dev/MySQL profile.
- Hibernate validates (but does not update) schemas in dev/default profiles. The H2 test profile is disposable and uses `ddl-auto=create-drop`; never start that profile against a persistent database.
