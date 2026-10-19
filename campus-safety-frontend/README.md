# Campus Safety Frontend

React + Vite browser app for student, security-officer, and administrator campus-safety workflows. The app connects to the Spring Boot API through Axios; operational dashboard data is loaded from the backend rather than bundled sample records.

## Local development

From Windows Command Prompt:

```cmd
cd /d "C:\AI-Powered Campus Safety-1\campus-safety-frontend"
npm.cmd install
set VITE_API_URL=http://localhost:8080/api/v1
npm.cmd run dev -- --host 127.0.0.1
```

The backend must be available at the configured API URL. Full MySQL and backend startup instructions are in the repository [README](../README.md).

## Checks

```cmd
npm.cmd run build
npm.cmd run lint
npm.cmd run test:e2e
```

`test:e2e` starts a Java 17 Spring Boot test-profile server using isolated in-memory H2 and a local Vite server. Playwright Chromium must be installed once with `npx.cmd playwright install chromium`. E2E admin credentials and the JWT signing key are generated for each run; the identity and database are test-only.

## AI behavior

The incident category and severity helper uses deterministic local keyword rules. Outputs are recommendations for human review, not external AI analysis or a source of campus statistics. No external provider is integrated yet.
