import { expect, test } from '@playwright/test'

test('a student is denied before authentication, then gets a local recommendation and tracks a persisted incident', async ({ page, request }) => {
  const email = `reporter-${crypto.randomUUID()}@example.edu`
  const description = `A student has an injury near the library ${crypto.randomUUID()}`
  const unauthenticatedResponse = await request.get('http://127.0.0.1:8080/api/v1/incidents')
  expect(unauthenticatedResponse.status()).toBe(401)

  await page.goto('/')
  await page.getByRole('button', { name: 'Need an account? Register as a student' }).click()
  await page.getByLabel('First name').fill('Jordan')
  await page.getByLabel('Last name').fill('Reporter')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill('CampusIncident#2026')
  await page.getByRole('button', { name: 'Create student account' }).click()

  await page.getByRole('button', { name: 'Incidents' }).click()
  await page.getByLabel('Category').first().selectOption('MEDICAL_EMERGENCY')
  await page.getByLabel('What happened?').fill(description)
  await page.getByLabel('Location').fill('Main Library')
  await page.getByLabel('Severity').first().selectOption('HIGH')
  await page.getByRole('button', { name: 'Get AI recommendation' }).click()
  await expect(page.getByText('AI suggestion — human review required')).toBeVisible()
  await expect(page.locator('.recommendation')).toContainText('Medical Emergency')
  await page.getByRole('button', { name: 'Submit incident' }).click()
  await expect(page.locator('.success-banner')).toContainText('Incident submitted and saved.')
  await expect(page.getByText(description)).toBeVisible()

  await page.getByRole('button', { name: 'Sign out' }).click()
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill('CampusIncident#2026')
  await page.getByRole('button', { name: 'Sign in' }).click()
  await page.getByRole('button', { name: 'Incidents' }).click()
  await expect(page.getByText(description)).toBeVisible()
  await page.getByRole('button', { name: 'History' }).first().click()
  await expect(page.getByRole('dialog')).toContainText('Created')
})
