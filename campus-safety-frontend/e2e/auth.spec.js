import { expect, test } from '@playwright/test'

test('registers a student, signs out, and logs back in', async ({ page }) => {
  const email = `student-${crypto.randomUUID()}@example.edu`

  await page.goto('/')
  await page.getByRole('button', { name: 'Need an account? Register as a student' }).click()
  await page.getByLabel('First name').fill('Casey')
  await page.getByLabel('Last name').fill('Student')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill('CampusFlow#2026')
  await page.getByRole('button', { name: 'Create student account' }).click()
  await expect(page.getByText('Campus safety command · Student')).toBeVisible()

  await page.getByRole('button', { name: 'Sign out' }).click()
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill('CampusFlow#2026')
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByText('Campus safety command · Student')).toBeVisible()
  await expect(page.getByRole('heading', { name: 'My recent incidents' })).toBeVisible()
})

test('shows a visible error for invalid login credentials', async ({ page }) => {
  const email = `recovery-${crypto.randomUUID()}@example.edu`

  await page.goto('/')
  await page.getByLabel('Email').fill(`missing-${crypto.randomUUID()}@example.edu`)
  await page.getByLabel('Password').fill('WrongPassword#2026')
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByRole('alert')).toContainText('Authentication failed')

  await page.getByRole('button', { name: 'Need an account? Register as a student' }).click()
  await page.getByLabel('First name').fill('Alex')
  await page.getByLabel('Last name').fill('Recovery')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill('CampusRecovery#2026')
  await page.getByRole('button', { name: 'Create student account' }).click()
  await expect(page.getByText('Campus safety command · Student')).toBeVisible()
  await page.getByRole('button', { name: 'Sign out' }).click()
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill('CampusRecovery#2026')
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByText('Campus safety command · Student')).toBeVisible()
})
