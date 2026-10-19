import { randomBytes } from 'node:crypto'
import { defineConfig, devices } from '@playwright/test'

const adminEmail = 'e2e-admin@example.edu'
const adminPassword = randomBytes(32).toString('base64url')

process.env.JWT_SECRET = randomBytes(32).toString('base64')
process.env.BOOTSTRAP_ADMIN_PASSWORD = adminPassword
process.env.E2E_ADMIN_EMAIL = adminEmail
process.env.E2E_ADMIN_PASSWORD = adminPassword

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://127.0.0.1:5173',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: [
    {
      command: 'cmd /c e2e\\start-test-backend.cmd',
      url: 'http://127.0.0.1:8080/actuator/health',
      reuseExistingServer: false,
      timeout: 120_000,
    },
    {
      command: 'npm.cmd run dev -- --host 127.0.0.1 --port 5173 --strictPort',
      url: 'http://127.0.0.1:5173',
      reuseExistingServer: false,
      timeout: 60_000,
    },
  ],
})
