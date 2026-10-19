import { expect, test } from '@playwright/test'

const adminEmail = process.env.E2E_ADMIN_EMAIL
const adminPassword = process.env.E2E_ADMIN_PASSWORD

async function registerStudent(page, email, description) {
  await page.goto('/')
  await page.getByRole('button', { name: 'Need an account? Register as a student' }).click()
  await page.getByLabel('First name').fill('Morgan')
  await page.getByLabel('Last name').fill('Responding')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill('CampusRoleFlow#2026')
  await page.getByRole('button', { name: 'Create student account' }).click()
  await page.getByRole('button', { name: 'Incidents' }).click()
  await page.getByLabel('Category').first().selectOption('SUSPICIOUS_ACTIVITY')
  await page.getByLabel('What happened?').fill(description)
  await page.getByLabel('Location').fill('Science Hall')
  await page.getByLabel('Severity').first().selectOption('HIGH')
  await page.getByRole('button', { name: 'Submit incident' }).click()
  await expect(page.getByText(description)).toBeVisible()
}

async function signIn(page, email, password) {
  await page.goto('/')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill(password)
  await page.getByRole('button', { name: 'Sign in' }).click()
}

test('admin promotes a reporter and an officer assigns, responds, and records history', async ({ browser, request }) => {
  const suffix = crypto.randomUUID()
  const studentEmail = `workflow-${suffix}@example.edu`
  const description = `Suspicious access attempt ${suffix}`
  const studentContext = await browser.newContext()
  const adminContext = await browser.newContext()
  const officerContext = await browser.newContext()
  const studentPage = await studentContext.newPage()
  const adminPage = await adminContext.newPage()
  const officerPage = await officerContext.newPage()

  try {
    await registerStudent(studentPage, studentEmail, description)
    await expect(studentPage.getByRole('button', { name: 'Alerts' })).toBeVisible()
    const studentToken = await studentPage.evaluate(() => sessionStorage.getItem('campus-safety-token'))
    const forbiddenAdminView = await request.get('http://127.0.0.1:8080/api/v1/admin/dashboard', {
      headers: { Authorization: `Bearer ${studentToken}` },
    })
    expect(forbiddenAdminView.status()).toBe(403)

    await signIn(adminPage, adminEmail, adminPassword)
    await expect(adminPage.getByRole('heading', { name: 'User access management' })).toBeVisible()
    const roleSelect = adminPage.getByRole('combobox', { name: `Role for ${studentEmail}` })
    await roleSelect.selectOption('SECURITY_OFFICER')
    await expect(adminPage.locator('.success-banner')).toContainText(`Role updated for ${studentEmail}`)

    await signIn(officerPage, studentEmail, 'CampusRoleFlow#2026')
    await officerPage.getByRole('button', { name: 'Incidents' }).click()
    await expect(officerPage.getByRole('heading', { name: 'Incident response queue' })).toBeVisible()
    await expect(officerPage.getByRole('heading', { name: 'Report a safety incident' })).toHaveCount(0)

    const incident = officerPage.locator('.incident-item').filter({ hasText: description })
    const incidentHeading = await incident.locator('h4').innerText()
    const incidentId = incidentHeading.match(/^#(\d+)/)[1]
    await incident.getByRole('combobox', { name: `Assign incident ${incidentId}` }).selectOption({ label: 'Morgan Responding' })
    await incident.getByRole('button', { name: 'Assign' }).click()
    await expect(incident).toContainText('Assigned to Morgan Responding')

    await incident.getByRole('combobox', { name: `Update status for incident ${incidentId}` }).selectOption('ACKNOWLEDGED')
    await expect(incident).toContainText('Acknowledged')
    await incident.getByRole('button', { name: 'History' }).click()
    const history = officerPage.getByRole('dialog')
    await expect(history).toContainText('Assigned')
    await expect(history).toContainText('Status: NEW -> ACKNOWLEDGED')
    await history.getByLabel('Add a response note').fill('Response team checked the east entrance.')
    await history.getByRole('button', { name: 'Add note' }).click()
    await expect(officerPage.locator('.success-banner')).toContainText('Response note added.')
    await expect(history).toContainText('Note Added')

    await studentPage.getByRole('button', { name: 'Notifications' }).click()
    await studentPage.getByRole('button', { name: 'Refresh' }).click()
    await expect(studentPage.getByText(`Your incident #${incidentId} status changed to ACKNOWLEDGED`)).toBeVisible()
  } finally {
    await Promise.all([studentContext.close(), adminContext.close(), officerContext.close()])
  }
})
